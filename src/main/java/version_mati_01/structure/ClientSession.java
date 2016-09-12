package version_mati_01.structure;

import version_mati_01.core.filter.IoFilterChain;
import version_mati_01.core.listener.SupportIoListener;
import version_mati_01.core.service.IoHandler;
import version_mati_01.core.service.IoProcessor;
import version_mati_01.core.service.IoService;
import version_mati_01.core.session.IoSession;
import version_mati_01.core.session.IoSessionConfig;
import version_mati_01.core.write.WriteFuture;
import version_mati_01.core.write.WriteRequest;
import version_mati_01.util.SessionUtil;
import version_mati_01.core.write.WriteRequestQueue;


import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mati on 09/09/16.
 */
public class ClientSession implements IoSession{

    private final long sessionId;
    /** Channel */
    private final SocketChannel socketChannel;
    /** The associated handler (EndPoint) */
    private final IoHandler handler;
    /** The associated processor with this session */
    private IoProcessor<ClientSession> processor;
    /** The SelectionKey used for this session */
    private SelectionKey selectionKey;
    /** FilterChain */
    private IoFilterChain filterChain;

    //Status variables
    private AtomicBoolean scheduledForFlush = new AtomicBoolean();;
    private boolean readSuspended;
    private boolean writeSuspended;
    private boolean closing;

    /** Session configuration */
    private IoSessionConfig config;
    /** Write request queue */
    private WriteRequestQueue writeRequestQueue;
    private WriteRequest currentWriteRequest;

    /** Lock object */
    private Object lock;


    public static ClientSession clientSessionFactory(SocketChannel socketChannel, IoHandler ioHandler,IoSessionConfig defaultSessionConfig,WriteRequestQueue writeRequestQueue){
        return new ClientSession(SessionUtil.sessionIdGenerator(),socketChannel,ioHandler,defaultSessionConfig,writeRequestQueue);
    }

    private ClientSession(long sessionId, SocketChannel socketChannel, IoHandler handler, IoSessionConfig defaultSessionConfig,WriteRequestQueue writeRequestQueue) {
        this.sessionId = sessionId;
        this.socketChannel = socketChannel;
        this.handler = handler;
        this.config = defaultSessionConfig;
        this.writeRequestQueue = writeRequestQueue;
    }

    @Override
    public long getId() {
        return sessionId;
    }

    @Override
    public IoHandler getIoHandler() {
        return handler;
    }

    public IoProcessor getProcessor() {
        return processor;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public void setProcessor(IoProcessor processor) {
        this.processor = processor;
    }

    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getRemoteAddress() {
        if (socketChannel == null) {
            return null;
        }

        Socket socket = socketChannel.socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getLocalAddress() {
        if (socketChannel == null) {
            return null;
        }

        Socket socket = socketChannel.socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isActive() {
        return selectionKey.isValid();
    }


    /**
     *  Metodo para armar el writeRequest.
     *  El mensaje llega luego de ser filtrado por los distintos Decoder y filtros.
     *  Acá se arma el Write request que luego será enviado.
     *
     * @param message
     * @throws Exception
     */
    @Override
    public void write(Object message) throws Exception {
        if (message == null) {
            throw new IllegalArgumentException("Trying to write a null message : not allowed");
        }

        // If the session has been closed or is closing, we can't either
        // send a message to the remote side. We generate a future
        // containing an exception.
        if (!selectionKey.isValid() && !selectionKey.isWritable()) {
            //todo: en un futuro esto va a tener un future..
//            WriteFuture future = new DefaultWriteFuture(this);
//            WriteRequest request = new DefaultWriteRequest(message, future, remoteAddress);
//            WriteException writeException = new WriteToClosedSessionException(request);
//            future.setException(writeException);
//            return future;
            throw new Exception("Connection is not available");
        }

        WriteRequest writeRequest = new DefaultWriteRequest(message);
        processor.write(this,writeRequest);


    }

    public boolean setScheduledForFlush(boolean schedule) {
        if (schedule) {
            // If the current tag is set to false, switch it to true,
            // otherwise, we do nothing but return false : the session
            // is already scheduled for flush
            return scheduledForFlush.compareAndSet(false, schedule);
        }

        scheduledForFlush.set(schedule);
        return true;
    }

    public SocketChannel getChannel() {
        return socketChannel;
    }

    public boolean isReadSuspended() {
        return readSuspended;
    }

    public void setReadSuspended(boolean readSuspended) {
        this.readSuspended = readSuspended;
    }

    public boolean isWriteSuspended() {
        return writeSuspended;
    }

    public void setWriteSuspended(boolean writeSuspended) {
        this.writeSuspended = writeSuspended;
    }

    public IoSessionConfig getConfig() {
        return config;
    }

    public void setConfig(IoSessionConfig config) {
        this.config = config;
    }

    public void increaseReadBufferSize() {
        //todo hacer esto
    }

    public void decreaseReadBufferSize() {
        //todo hacer esto
    }

    public WriteRequestQueue getWriteRequestQueue() {
        return writeRequestQueue;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    //todo: este metodo deberia ir en la clase base..
    public void addWriteRequest(WriteRequest writeRequest) {
        writeRequestQueue.offer(writeRequest);
    }

    public void unscheduledForFlush() {
        scheduledForFlush.set(false);
    }

    //todo: ver esto..
    public boolean isConnected() {
        return selectionKey.isValid();
    }

    public WriteRequest getCurrentWriteRequest() {
        return currentWriteRequest;
    }

    public void setCurrentWriteRequest(WriteRequest currentWriteRequest) {
        this.currentWriteRequest = currentWriteRequest;
    }

    //todo: este metodo podria tener un future tambien..
    public final void closeNow() {
        synchronized (lock) {
            if (isClosing()) {
//                return closeFuture;
            }

            closing = true;
        }


//        getFilterChain().fireFilterClose();
        try {
            //esto no creo que esté bien..
            handler.sessionClosed(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        return closeFuture;
    }

    public boolean isClosing() {
        return closing;
    }

    public boolean isScheduledForFlush() {
        return scheduledForFlush.get();
    }
}
