package version_01.core.polling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.listener.SupportIoListener;
import version_01.core.service.IoProcessor;
import version_01.core.service.IoService;
import version_01.core.session.IoSession;
import version_01.core.session.SessionState;
import version_01.core.write.SessionCloseException;
import version_01.structure.processors.interfaces.ReadManager;
import version_01.structure.processors.interfaces.WriteManager;
import version_01.structure.session.BaseSession;
import version_01.structure.session.IoSessionIterator;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mati on 22/09/16.
 */
public abstract class AbstractProcessor<S extends IoSession> implements IoProcessor<S>{

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(AbstractProcessor.class);
    /** Processor id*/
    private final int processorId;
    /** Service base */
    private IoService ioService;
    /** A queue used to store new sessions in waiting state */
    private Queue<S> newSessions = new ConcurrentLinkedDeque<>();
    /** A queue used to store the sessions to be removed */
    private final Queue<S> removingSessions = new ConcurrentLinkedQueue<>();
    /** A queue used to store the sessions to be flushed */
    private final Queue<S> flushingSessions = new ConcurrentLinkedQueue<>();
    /** Support listener to fire session events */
    private SupportIoListener supportIoListener;
    /**
     * A timeout used for the select, as we need to get out to deal with idle
     * sessions
     */
    private static final long SELECT_TIMEOUT = 1000L;
    /** Helpers Managers */
    private ReadManager readManager;
    private WriteManager writeManager;
    /** status flags */
    private AtomicBoolean isRunning = new AtomicBoolean();

    public AbstractProcessor(IoService ioService, int id, ReadManager readManager, WriteManager writeManager) {
        this.processorId = id;
        this.ioService = ioService;
        this.readManager = readManager;
        this.writeManager = writeManager;
        defaultInit();
    }

    private void defaultInit(){
        readManager.setProcessor(this);
        writeManager.setProcessor(this);
    }


    /**
     * Get the inner Processor, asking the executor to pick a thread in its
     * pool. The Runnable will be renamed
     */
    public Runnable startupProcessorRunnable(){
        return new Processor();
    }

    public int getProcessorId() {
        return processorId;
    }


    @Override
    public void add(S session) {
        newSessions.offer(session);
    }

    @Override
    public void flush(S session) {
        // add the session to the queue if it's not already
        // in the queue, then wake up the select()
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            wakeup();
        }
    }

    private void wakeup() {
        getSelector().wakeup();
    }

    /**
     * Write all the pending messages
     */
    private void flush(long currentTime) {
        if (flushingSessions.isEmpty()) {
            return;
        }

        do {
            S session = flushingSessions.poll(); // the same one with
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
                        boolean flushedAll = writeManager.flushNow(session, currentTime);

                        if (flushedAll && !session.getWriteRequestQueue().isEmpty()
                                && !session.isScheduledForFlush()) {
                            scheduleFlush(session);
                        }
                    } catch (Exception e) {
                        scheduleRemove(session);
                        session.closeNow();
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

    public void scheduleFlush(S session) {
        // add the session to the queue if it's not already
        // in the queue
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
        }
    }

    @Override
    public void updateTrafficControl(S session) {
        // nothing.. esto es para cuando quiera controlar las estadisticas
    }

    @Override
    public void remove(S session) {
        scheduleRemove(session);
    }


    /**
     * Schedule remove session
     * @param session
     */
    private void scheduleRemove(S session) {
        LOG.debug("scheduleRemove session: "+session);
        if (!removingSessions.contains(session)) {
            removingSessions.add(session);
        }
    }

    protected int select(long timeout) throws Exception {
        return getSelector().select(timeout);
    }

    private int select() throws IOException {
        return getSelector().select();
    }

    protected abstract Iterator<S> selectedSessions();

    /**
     * Inicia la sesion registrandola en el selector
     * @param session
     * @throws Exception
     */
    protected void init(S session) throws Exception {
        SelectableChannel ch = session.getChannel();
        ch.configureBlocking(false);
        session.setSelectionKey(ch.register(getSelector(), SelectionKey.OP_READ, session));
    }

    public void destroy(IoSession session) throws Exception {
        LOG.debug("Destroying session: "+session);
        ByteChannel ch = (ByteChannel) session.getChannel();
        SelectionKey key = session.getSelectionKey();
        if (key != null) {
            key.cancel();
        }
        ch.close();
    }

    protected boolean isSelectorEmpty() {
        return getSelector().keys().isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isBrokenConnection() throws IOException {
        // A flag set to true if we find a broken session
        boolean brokenSession = false;

        synchronized (getSelector()) {
            // Get the selector keys
            Set<SelectionKey> keys = getSelector().keys();
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
    protected abstract void registerNewSelector() throws IOException;

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
                S session = newSessions.poll();
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
    private boolean addNow(S session) {
        boolean registered = false;

        try {
            init(session);
            registered = true;

            // Build the filter chain of this session.
            preInitSession(session);

            // DefaultIoFilterChain.CONNECT_FUTURE is cleared inside here
            // in AbstractIoFilterChain.fireSessionOpened().
            // Propagate the SESSION_CREATED event up to the chain
            supportIoListener.fireSessionCreated(session);


        } catch (Exception e) {
            supportIoListener.fireExceptionCaught(session,"Exception during the process new session",e);
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

    /**
     * Load session specific params, like filters,conf, etc..
     * @param session
     */
    protected abstract void preInitSession(S session) throws Exception;

    private void process() throws Exception {
        for (Iterator<S> i = selectedSessions(); i.hasNext();) {
            S session = i.next();
            process(session);
            i.remove();
        }
    }

    /**
     * Deal with session ready for the read or write operations, or both.
     */
    private void process(S session) {
        // Process Reads
        if (isReadable(session) && !session.isReadSuspended()) {
            readManager.read(session);
        }

        // Process writes
        if (isWritable(session) && !session.isWriteSuspended()) {
            // add the session to the queue, if it's not already there
            if (session.setScheduledForFlush(true)) {
                flushingSessions.add(session);
            }
        }
    }

    protected boolean isReadable(S session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && key.isReadable();
    }

    protected boolean isWritable(S session) {
        SelectionKey key = session.getSelectionKey();

        return (key != null) && key.isValid() && key.isWritable();
    }

    /**
     * Change the interest in OP_WRITE
     *
     * @param session
     * @param interest
     */
    public void setInterestedInWrite(BaseSession session, boolean interest) throws SessionCloseException {
        if (session.isActive() && session.isConnected()) {
            SelectionKey selectionKey = session.getSelectionKey();
            if (interest) {
                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
            } else {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            }
        }else {
            throw new SessionCloseException("Cant setInterestInWrite because the session is closed.");
        }
    }

    private int removeSessions() {
        int removedSessions = 0;

        for (S session = removingSessions.poll(); session != null;session = removingSessions.poll()) {
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

    private boolean removeNow(S session) {
        //todo: este metodo le manda al handler los mensajes que no puedieron ser enviados seteando el future del write request con la excepcion y removiendolos de la WriteRequestQueue
//        clearWriteRequestQueue(session);
        try {
            destroy(session);
            return true;
        } catch (Exception e) {
            try {
                supportIoListener.fireExceptionCaught(session,"Exception during removeNow method",e);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                //todo: ver este metodo
//                clearWriteRequestQueue(session);
                supportIoListener.fireSessionDestroyed(session);
            } catch (Exception e) {
                // The session was either destroyed or not at this point.
                // We do not want any exception thrown from this "cleanup" code to change
                // the return value by bubbling up.
                supportIoListener.fireExceptionCaught(session,null,e);

            }
        }
        return false;
    }

    protected SessionState getState(S session) {
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

                        for (Iterator<S> i = allSessions(); i.hasNext();) {
                            S session = i.next();

                            if (session.isActive()) {
                                scheduleRemove(session);
                                hasKeys = true;
                            }
                        }

                        if (hasKeys) {
                            //todo: ver wakeup..
                            wakeup();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

        }
    }

    private Iterator<S> allSessions() {
        return new IoSessionIterator<>(getSelector().keys());
    }

    public void setSupportIoListener(SupportIoListener supportIoListener) {
        this.supportIoListener = supportIoListener;
    }

    /**
     * Absract method to obtain the selector
     * @return
     */
    public abstract Selector getSelector();

    @Override
    public SupportIoListener getSupportIoListener() {
        return supportIoListener;
    }


    @Override
    public String toString() {
        return "NioNodeIoProcessor{" +
                "processorId=" + processorId +
                '}';
    }
}
