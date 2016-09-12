package version_mati_01.core.polling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_mati_01.core.filter.IoFilterChainBuilder;
import version_mati_01.core.listener.SupportIoListener;
import version_mati_01.core.service.IoProcessor;
import version_mati_01.core.service.IoService;
import version_mati_01.core.session.IoSession;
import version_mati_01.core.session.IoSessionConfig;
import version_mati_01.core.session.SessionState;
import version_mati_01.structure.ClientSession;
import version_mati_01.core.write.WriteRequest;
import version_mati_01.core.write.WriteRequestQueue;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by mati on 09/09/16.
 */
public class NioNodeIoProcessor<S extends ClientSession> implements IoProcessor<ClientSession> {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(NioNodeIoProcessor.class);
    /** Processor id*/
    private final int processorId;
    /** A queue used to store new sessions in waiting state */
    private Queue<ClientSession> newSessions = new ConcurrentLinkedDeque<>();
    /** A queue used to store the sessions to be removed */
    private final Queue<ClientSession> removingSessions = new ConcurrentLinkedQueue<>();
    /** A queue used to store the sessions to be flushed */
    private final Queue<ClientSession> flushingSessions = new ConcurrentLinkedQueue<>();
    /** Selector */
    private Selector selector;
    /** Service base */
    private IoService ioService;
    /** Support listener to fire session events */
    private SupportIoListener supportIoListener;
    /**
     * A timeout used for the select, as we need to get out to deal with idle
     * sessions
     */
    private static final long SELECT_TIMEOUT = 1000L;

    /** status flags */
    private AtomicBoolean isRunning = new AtomicBoolean();


    public NioNodeIoProcessor(IoService ioService,int id) {
        this.processorId = id;
        this.ioService = ioService;
        init();
    }

    private void init(){

        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a selector.",e);
        }


    }

    /**
     * Get the inner Processor, asking the executor to pick a thread in its
     * pool. The Runnable will be renamed
     */
    public Runnable startupProcessorRunnable() {
        Processor processor = new Processor();
        return processor;
        // Just stop the select() and start it again, so that the processor
        // can be activated immediately.
//        wakeup();
    }

    public int getProcessorId() {
        return processorId;
    }

    @Override
    public boolean isDisposing() {
        return false;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void dispose() throws Exception {
        selector.close();
    }

    @Override
    public void add(ClientSession session) {
        newSessions.offer(session);
    }

    @Override
    public void flush(ClientSession session) {
        // add the session to the queue if it's not already
        // in the queue, then wake up the select()
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
//            wakeup();
        }
    }

    @Override
    public void write(ClientSession session, WriteRequest writeRequest) {
        supportIoListener.fireSendMessage(this,session,writeRequest);
    }

    //todo: Esto es la parte de escritura, primero voy a probar la lectura..
    /**
     * Write all the pending messages
     */
    private void flush(long currentTime) {
        if (flushingSessions.isEmpty()) {
            return;
        }

        do {
            ClientSession session = flushingSessions.poll(); // the same one with
            // firstSession

            if (session == null) {
                // Just in case ... It should not happen.
                break;
            }

            // Reset the Schedule for flush flag for this session,
            // as we are flushing it now
            session.unscheduledForFlush();

            SessionState state = getState(session);

            switch (state) {
                case OPENED:
                    try {
                        boolean flushedAll = flushNow(session, currentTime);

                        if (flushedAll && !session.getWriteRequestQueue().isEmpty()
                                && !session.isScheduledForFlush()) {
                            scheduleFlush(session);
                        }
                    } catch (Exception e) {
                        scheduleRemove(session);
                        session.closeNow();
//                        IoFilterChain filterChain = session.getFilterChain();
//                        filterChain.fireExceptionCaught(e);
                        supportIoListener.fireExceptionCaught(session,"Exception putting to flush session",e);
                    }

                    break;

                case CLOSING:
                    // Skip if the channel is already closed.
                    break;

                case OPENING:
                    // Retry later if session is not yet fully initialized.
                    // (In case that Session.write() is called before addSession()
                    // is processed)
                    scheduleFlush(session);
                    return;

                default:
                    throw new IllegalStateException(String.valueOf(state));
            }

        } while (!flushingSessions.isEmpty());
    }

    private void scheduleFlush(ClientSession session) {
        // add the session to the queue if it's not already
        // in the queue
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
        }
    }

    private boolean flushNow(ClientSession session, long currentTime) {
        if (!session.isConnected()) {
            scheduleRemove(session);
            return false;
        }

        final boolean hasFragmentation = false;//session.getTransportMetadata().hasFragmentation();

        final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();

        // Set limitation for the number of written bytes for read-write
        // fairness. I used maxReadBufferSize * 3 / 2, which yields best
        // performance in my experience while not breaking fairness much.
        final int maxWrittenBytes = session.getConfig().getMaxReadBufferSize()
                + (session.getConfig().getMaxReadBufferSize() >>> 1);

        int writtenBytes = 0;
        WriteRequest writeRequest = null;

        try {
            // Clear OP_WRITE , todo: ver porqué hace esto antes de ponerlo..
//            setInterestedInWrite(session, false);

            do {
                // Check for pending writes.
                writeRequest = session.getCurrentWriteRequest();

                if (writeRequest == null) {
                    writeRequest = writeRequestQueue.poll();

                    if (writeRequest == null) {
                        break;
                    }

                    session.setCurrentWriteRequest(writeRequest);
                }

                int localWrittenBytes = 0;
                Object message = writeRequest.getMessageFiltered();

                if (message instanceof ByteBuffer) {
                    localWrittenBytes = writeBuffer(session, writeRequest, hasFragmentation, maxWrittenBytes - writtenBytes,
                            currentTime);

                    if ((localWrittenBytes > 0) && ((ByteBuffer) message).hasRemaining()) {
                        // the buffer isn't empty, we re-interest it in writing
                        writtenBytes += localWrittenBytes;
                        //todo: ver porqué hace esto, yo supongo que es para volver a mandar en la otra vuelta del ciclo cuando fragmenta la data
//                        setInterestedInWrite(session, true);
                        return false;
                    }
                } else {
                    throw new IllegalStateException("Don't know how to handle message of type '"
                            + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
                }

                if (localWrittenBytes == 0) {
                    // Kernel buffer is full.
                    //todo: ver porqué hace esto, yo supongo que es para volver a mandar en la otra vuelta del ciclo cuando fragmenta la data
//                    setInterestedInWrite(session, true);
                    return false;
                }

                writtenBytes += localWrittenBytes;

                if (writtenBytes >= maxWrittenBytes) {
                    // Wrote too much
                    scheduleFlush(session);
                    return false;
                }


            } while (writtenBytes < maxWrittenBytes);
        } catch (Exception e) {
            if (writeRequest != null) {
                if (writeRequest.getFuture()!=null)
                    writeRequest.getFuture().setException(e);
            }

//            IoFilterChain filterChain = session.getFilterChain();
//            filterChain.fireExceptionCaught(e);
            supportIoListener.fireExceptionCaught(session,"Exception trying flush session",e);
            return false;
        }

        return true;
    }


    private int writeBuffer(ClientSession session, WriteRequest writeRequest, boolean hasFragmentation, int maxLength, long currentTime)
            throws Exception {
        ByteBuffer buf = (ByteBuffer) writeRequest.getMessageFiltered();
        int localWrittenBytes = 0;

        if (buf.hasRemaining()) {
            int length;

            if (hasFragmentation) {
                length = Math.min(buf.remaining(), maxLength);
            } else {
                length = buf.remaining();
            }

            try {
                localWrittenBytes = write(session, buf, length);
            } catch (IOException ioe) {
                // We have had an issue while trying to send data to the
                // peer : let's close the session.
                buf = null;
                session.closeNow();
                destroy(session);

                return 0;
            }

        }

//        session.increaseWrittenBytes(localWrittenBytes, currentTime);

        if (!buf.hasRemaining() || (!hasFragmentation && (localWrittenBytes != 0))) {
            // Buffer has been sent, clear the current request.
            int pos = buf.position();
            if (buf.mark().position()>-1)
                buf.reset();

            supportIoListener.fireMessageSent(session, writeRequest);

            // And set it back to its position
            buf.position(pos);
        }

        return localWrittenBytes;
    }

    /** Write to the channel */
    protected int write(ClientSession session, ByteBuffer buf, int length) throws Exception {
        if (buf.remaining() <= length) {
            return session.getChannel().write(buf);
        }

        int oldLimit = buf.limit();
        buf.limit(buf.position() + length);
        try {
            return session.getChannel().write(buf);
        } finally {
            buf.limit(oldLimit);
        }
    }

    @Override
    public void updateTrafficControl(ClientSession session) {

    }

    @Override
    public void remove(ClientSession session) {
        scheduleRemove(session);
    }

    private void scheduleRemove(ClientSession session) {
        if (!removingSessions.contains(session)) {
            removingSessions.add(session);
        }
    }

    protected int select(long timeout) throws Exception {
        return selector.select(timeout);
    }

    private int select() throws IOException {
        return selector.select();
    }
    protected Iterator<ClientSession> selectedSessions() {
        return new IoSessionIterator(selector.selectedKeys());
    }

    protected void init(ClientSession session) throws Exception {
        SelectableChannel ch = session.getChannel();
        ch.configureBlocking(false);
        session.setSelectionKey(ch.register(selector, SelectionKey.OP_READ, session));
    }

    protected void destroy(ClientSession session) throws Exception {
        ByteChannel ch = session.getChannel();
        SelectionKey key = session.getSelectionKey();
        if (key != null) {
            key.cancel();
        }
        ch.close();
    }

    protected boolean isSelectorEmpty() {
        return selector.keys().isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    protected boolean isBrokenConnection() throws IOException {
        // A flag set to true if we find a broken session
        boolean brokenSession = false;

        synchronized (selector) {
            // Get the selector keys
            Set<SelectionKey> keys = selector.keys();
            // Loop on all the keys to see if one of them
            // has a closed channel
            for (SelectionKey key : keys) {
                SelectableChannel channel = key.channel();

                if (((channel instanceof SocketChannel) && !((SocketChannel) channel).isConnected())) {
                    // The channel is not connected anymore. Cancel
                    // the associated key then.
                    key.cancel();
                    // Set the flag to true to avoid a selector switch
                    brokenSession = true;
                }
            }
        }
        return brokenSession;
    }

    /**
     * In the case we are using the java select() method, this method is used to
     * trash the buggy selector and create a new one, registering all the
     * sockets on it.
     */
    protected void registerNewSelector() throws IOException {
        synchronized (selector) {
            Set<SelectionKey> keys = selector.keys();

            // Open a new selector
            Selector newSelector = Selector.open();


            // Loop on all the registered keys, and register them on the new selector
            for (SelectionKey key : keys) {
                SelectableChannel ch = key.channel();

                // Don't forget to attache the session, and back !
                ClientSession session = (ClientSession) key.attachment();
                SelectionKey newKey = ch.register(newSelector, key.interestOps(), session);
                session.setSelectionKey(newKey);
            }

            // Now we can close the old selector and switch it
            selector.close();
            selector = newSelector;
        }
    }

    /**
     * Loops over the new sessions blocking queue and returns the number of
     * sessions which are effectively created
     *
     * @return The number of new sessions
     */
    private int handleNewSessions() {
        int addedSessions = 0;
        if (!newSessions.isEmpty())
            for (int i=0;i<newSessions.size();i++){
                ClientSession session = newSessions.poll();
                if (session!=null){
                    if (addNow(session)) {
                        // A new session has been created
                        addedSessions++;
                    }
                }
            }
        return addedSessions;
    }

    /**
     * Process a new session : - initialize it - create its chain - fire the
     * CREATED listeners if any
     *
     * @param session The session to create
     * @return <tt>true</tt> if the session has been registered
     */
    private boolean addNow(ClientSession session) {
        boolean registered = false;

        try {
            init(session);
            registered = true;

            // Build the filter chain of this session.
//            IoFilterChainBuilder chainBuilder = ioService.getFilterChainBuilder();
//            chainBuilder.buildFilterChain(session.getFilterChain());

            // DefaultIoFilterChain.CONNECT_FUTURE is cleared inside here
            // in AbstractIoFilterChain.fireSessionOpened().
            // Propagate the SESSION_CREATED event up to the chain
            supportIoListener.fireSessionCreated(session);

//            IoServiceListenerSupport listeners = ((AbstractIoService) session.getService()).getListeners();
//            listeners.fireSessionCreated(session);
        } catch (Exception e) {
            e.printStackTrace();
//            ExceptionMonitor.getInstance().exceptionCaught(e);

            try {
                destroy(session);
            } catch (Exception e1) {
//                ExceptionMonitor.getInstance().exceptionCaught(e1);
                e1.printStackTrace();
            } finally {
                registered = false;
            }
        }

        return registered;
    }

    private void process() throws Exception {
        for (Iterator<ClientSession> i = selectedSessions(); i.hasNext();) {
            ClientSession session = i.next();
            process(session);
            i.remove();
        }
    }

    /**
     * Deal with session ready for the read or write operations, or both.
     */
    private void process(ClientSession session) {
        // Process Reads
        if (isReadable(session) && !session.isReadSuspended()) {
            read(session);
        }

        // Process writes
        if (isWritable(session) && !session.isWriteSuspended()) {
            // add the session to the queue, if it's not already there
            if (session.setScheduledForFlush(true)) {
                flushingSessions.add(session);
            }
        }
    }

    protected boolean isReadable(ClientSession session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && key.isReadable();
    }

    protected boolean isWritable(ClientSession session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && key.isWritable();
    }

    private void read(ClientSession session) {
        IoSessionConfig config = session.getConfig();
        int bufferSize = config.getReadBufferSize();
//        IoBuffer buf = IoBuffer.allocate(bufferSize);

        ByteBuffer buf = ByteBuffer.allocate(bufferSize);


        // por ahora lo dejo en false..
        final boolean hasFragmentation = false;//session.getTransportMetadata().hasFragmentation();

        try {
            int readBytes = 0;
            int readBuffAux;

            try {
                if (hasFragmentation) {

                    while ((readBuffAux = read(session, buf)) > 0) {
                        readBytes += readBuffAux;

                        if (!buf.hasRemaining()) {
                            break;
                        }
                    }
                } else {
                    readBuffAux = read(session, buf);

                    if (readBuffAux > 0) {
                        readBytes = readBuffAux;
                    }
                }
            } finally {
                buf.flip();
            }

            if (readBytes > 0) {
//                IoFilterChain filterChain = session.getFilterChain();
//                filterChain.fireMessageReceived(buf);
                // esto es para probar que esté llegando, despues tengo que poner el filtro

                //launch messageReceivedEvent
                supportIoListener.fireMessageReceived(session,buf);


                buf = null;

                if (hasFragmentation) {
                    if (readBytes << 1 < config.getReadBufferSize()) {
                        session.decreaseReadBufferSize();
                    } else if (readBytes == config.getReadBufferSize()) {
                        session.increaseReadBufferSize();
                    }
                }
            }

            if (readBuffAux < 0) {
                // scheduleRemove(session);
//                IoFilterChain filterChain = session.getFilterChain();
//                filterChain.fireInputClosed();
                session.getIoHandler().inputClosed(session);
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                if (!(e instanceof PortUnreachableException)) {
                    scheduleRemove(session);
                }
            }

//            IoFilterChain filterChain = session.getFilterChain();
//            filterChain.fireExceptionCaught(e);
            try {
                session.getIoHandler().exceptionCaught(session,e);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    protected int read(ClientSession session, ByteBuffer buf) throws Exception {
        ByteChannel channel = session.getChannel();
        return channel.read(buf);
    }

    /**
     * An encapsulating iterator around the {@link Selector#selectedKeys()} or
     * the {@link Selector#keys()} iterator;
     */
    protected static class IoSessionIterator<ClientSession> implements Iterator<ClientSession> {
        private final Iterator<SelectionKey> iterator;

        /**
         * Create this iterator as a wrapper on top of the selectionKey Set.
         *
         * @param keys
         *            The set of selected sessions
         */
        private IoSessionIterator(Set<SelectionKey> keys) {
            iterator = keys.iterator();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public ClientSession next() {
            SelectionKey key = iterator.next();
            ClientSession nioSession = (ClientSession) key.attachment();
            return nioSession;
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            iterator.remove();
        }
    }

    private int removeSessions() {
        int removedSessions = 0;

        for (ClientSession session = removingSessions.poll(); session != null;session = removingSessions.poll()) {
            SessionState state = getState(session);

            // Now deal with the removal accordingly to the session's state
            switch (state) {
                case OPENED:
                    // Try to remove this session
                    if (removeNow(session)) {
                        removedSessions++;
                    }

                    break;

                case CLOSING:
                    // Skip if channel is already closed
                    // In any case, remove the session from the queue
                    removedSessions++;
                    break;

                case OPENING:
                    // Remove session from the newSessions queue and
                    // remove it
                    newSessions.remove(session);

                    if (removeNow(session)) {
                        removedSessions++;
                    }

                    break;

                default:
                    throw new IllegalStateException(String.valueOf(state));
            }
        }

        return removedSessions;
    }

    private boolean removeNow(ClientSession session) {
        //todo: ver el metodo este
        //clearWriteRequestQueue(session);

        try {
            destroy(session);
            return true;
        } catch (Exception e) {
//            IoFilterChain filterChain = session.getFilterChain();
//            filterChain.fireExceptionCaught(e);
            try {
                session.getIoHandler().exceptionCaught(session,e);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                //todo: ver este metodo
//                clearWriteRequestQueue(session);

//                ((AbstractIoService) session.getService()).getListeners().fireSessionDestroyed(session);
                session.getIoHandler().sessionClosed(session);
            } catch (Exception e) {
                // The session was either destroyed or not at this point.
                // We do not want any exception thrown from this "cleanup" code to change
                // the return value by bubbling up.
//                IoFilterChain filterChain = session.getFilterChain();
//                filterChain.fireExceptionCaught(e);
                try {
                    session.getIoHandler().exceptionCaught(session,e);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }

        return false;
    }

    //todo: esto limpia la cola, tengo que armar la clase de la cola..
//    private void clearWriteRequestQueue(S session) {
//        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
//        WriteRequest req;
//
//        List<WriteRequest> failedRequests = new ArrayList<WriteRequest>();
//
//        if ((req = writeRequestQueue.poll(session)) != null) {
//            Object message = req.getMessage();
//
//            if (message instanceof IoBuffer) {
//                IoBuffer buf = (IoBuffer) message;
//
//                // The first unwritten empty buffer must be
//                // forwarded to the filter chain.
//                if (buf.hasRemaining()) {
//                    buf.reset();
//                    failedRequests.add(req);
//                } else {
//                    IoFilterChain filterChain = session.getFilterChain();
//                    filterChain.fireMessageSent(req);
//                }
//            } else {
//                failedRequests.add(req);
//            }
//
//            // Discard others.
//            while ((req = writeRequestQueue.poll(session)) != null) {
//                failedRequests.add(req);
//            }
//        }
//
//        // Create an exception and notify.
//        if (!failedRequests.isEmpty()) {
//            WriteToClosedSessionException cause = new WriteToClosedSessionException(failedRequests);
//
//            for (WriteRequest r : failedRequests) {
//                session.decreaseScheduledBytesAndMessages(r);
//                r.getFuture().setException(cause);
//            }
//
//            IoFilterChain filterChain = session.getFilterChain();
//            filterChain.fireExceptionCaught(cause);
//        }
//    }


    protected SessionState getState(ClientSession session) {
        SelectionKey key = session.getSelectionKey();

        if (key == null) {
            // The channel is not yet registred to a selector
            return SessionState.OPENING;
        }

        if (key.isValid()) {
            // The session is opened
            return SessionState.OPENED;
        } else {
            // The session still as to be closed
            return SessionState.CLOSING;
        }
    }

    private class Processor implements Runnable{

        @Override
        public void run() {

            isRunning.set(true);
            int nSessions = 0;

            for(;;){

                try {
                    long t0 = System.currentTimeMillis();
                    int selected = select(SELECT_TIMEOUT);
                    long t1 = System.currentTimeMillis();
                    long delta = (t1 - t0);

                    if ((selected == 0) && (delta < 100)) {
                        // Last chance : the select() may have been
                        // interrupted because we have had an closed channel.
                        if (isBrokenConnection()) {
                            LOG.warn("Broken connection");
                        } else {
                            LOG.warn("Create a new selector. Selected is 0, delta = " + (t1 - t0));
                            // Ok, we are hit by the nasty epoll
                            // spinning.
                            // Basically, there is a race condition
                            // which causes a closing file descriptor not to be
                            // considered as available as a selected channel,
                            // but
                            // it stopped the select. The next time we will
                            // call select(), it will exit immediately for the
                            // same
                            // reason, and do so forever, consuming 100%
                            // CPU.
                            // We have to destroy the selector, and
                            // register all the socket on a new one.
                            registerNewSelector();
                        }
                    }

                    // Manage newly created session first
                    nSessions += handleNewSessions();

                    // Now, if we have had some incoming or outgoing events,
                    // deal with them
                    if (selected > 0) {
                        // LOG.debug("Processing ..."); // This log hurts one of
                        // the MDCFilter test...
                        process();
                    }


                    // Write the pending requests
                    long currentTime = System.currentTimeMillis();
                    flush(currentTime);

                    // And manage removed sessions
                    nSessions -= removeSessions();

                    // Last, not least, write Idle events to the idle sessions
//                    notifyIdleSessions(currentTime);

                    // Get a chance to exit the infinite loop if there are no
                    // more sessions on this Processor
                    if (nSessions == 0) {
//                        processorRef.set(null);

                        if (newSessions.isEmpty() && isSelectorEmpty()) {
                            // newSessions.add() precedes startupProcessor
//                            assert (processorRef.get() != this);
//                            break;
                        }

//                        assert (processorRef.get() != this);

//                        if (!processorRef.compareAndSet(null, this)) {
                            // startupProcessor won race, so must exit processor
//                            assert (processorRef.get() != this);
//                            break;
//                        }

//                        assert (processorRef.get() == this);
                    }

                    // Disconnect all sessions immediately if disposal has been
                    // requested so that we exit this loop eventually.
                    if (isDisposing()) {
                        boolean hasKeys = false;

                        for (Iterator<ClientSession> i = allSessions(); i.hasNext();) {
                            ClientSession session = i.next();

                            if (session.isActive()) {
                                scheduleRemove(session);
                                hasKeys = true;
                            }
                        }

                        if (hasKeys) {
                            //todo: ver wakeup..
//                            wakeup();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }





            }




        }
    }

    private Iterator<ClientSession> allSessions() {
        return new IoSessionIterator<>(selector.keys());
    }

    public void setSupportIoListener(SupportIoListener supportIoListener) {
        this.supportIoListener = supportIoListener;
    }
}
