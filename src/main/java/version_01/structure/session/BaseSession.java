package version_01.structure.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.configuration.ServerPortType;
import version_01.core.service.IoHandler;
import version_01.core.service.IoProcessor;
import version_01.core.session.IoSession;
import version_01.core.session.IoSessionAttributeMap;
import version_01.core.session.IoSessionConfig;
import version_01.core.write.SessionCloseException;
import version_01.core.write.WriteRequest;
import version_01.util.SessionUtil;
import version_01.core.write.WriteRequestQueue;


import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mati on 09/09/16.
 */
public class BaseSession implements IoSession{

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(BaseSession.class);

    private final long sessionId;
    /** Channel */
    private final SocketChannel socketChannel;
    /** The associated handler (EndPoint) */
    private final IoHandler handler;
    /** The associated processor with this session */
    private IoProcessor<BaseSession> processor;
    /** The SelectionKey used for this session */
    private SelectionKey selectionKey;
    /** Session data */
    private IoSessionAttributeMap attributeMap;
    /** Session Role */
    private ServerPortType serverPortType;

    //Status variables
    private AtomicBoolean scheduledForFlush = new AtomicBoolean();;
    private boolean readSuspended;
    private boolean writeSuspended;
    private boolean closing;

    /** Session configuration */
    private IoSessionConfig config;
    /** Write request queue */
    private WriteRequestQueue writeRequestQueue;
    /** current writeRequest wainting to be send */
    private WriteRequest currentWriteRequest;

    /** Lock object */
    private Object lock = new Object();

    /** Flag to set to true when SSL is enabled */
    private boolean isSecure;


    public static BaseSession clientSessionFactory(ServerPortType serverPortType, SocketChannel socketChannel, IoHandler ioHandler, IoSessionConfig defaultSessionConfig, WriteRequestQueue writeRequestQueue){
        return new BaseSession(SessionUtil.sessionIdGenerator(),serverPortType,socketChannel,ioHandler,defaultSessionConfig,writeRequestQueue);
    }

    private BaseSession(long sessionId, ServerPortType serverPortType, SocketChannel socketChannel, IoHandler handler, IoSessionConfig defaultSessionConfig, WriteRequestQueue writeRequestQueue) {
        this.sessionId = sessionId;
        this.socketChannel = socketChannel;
        this.handler = handler;
        this.config = defaultSessionConfig;
        this.writeRequestQueue = writeRequestQueue;
        this.serverPortType = serverPortType;
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
    public void write(Object message) throws SessionCloseException {
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
            throw new SessionCloseException("Connection is not available");
        }

        WriteRequest writeRequest = new DefaultWriteRequest(message);
        processor.write(this,writeRequest);


    }

    @Override
    public void close() {
        LOG.info("session close, id: "+getId());
        setScheduledForFlush(true);

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
        LOG.info("CloseNow session: "+this);
        synchronized (lock) {
            if (isClosing()) {
//                return closeFuture;
            }

            closing = true;
        }

        //removing session
        processor.remove(this);

//        try {
//            //esto no creo que esté bien..
//            handler.sessionClosed(this);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return closeFuture;
    }

    public boolean isClosing() {
        return closing;
    }

    public boolean isScheduledForFlush() {
        return scheduledForFlush.get();
    }

    public void setSecure(boolean secure) {
        isSecure = secure;
    }

    public boolean isSecure() {
        return isSecure;
    }


    public void setAttributeMap(IoSessionAttributeMap attributeMap) {
        this.attributeMap = attributeMap;
    }

    /**
     * {@inheritDoc}
     */
    public final Object getAttribute(Object key, Object defaultValue) {
        return attributeMap.getAttribute(this, key, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public final Object getAttribute(Object key) {
        return getAttribute(key, null);
    }
    /**
     * {@inheritDoc}
     */
    public final Object setAttribute(Object key, Object value) {
        return attributeMap.setAttribute(this, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttribute(Object key) {
        return setAttribute(key, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttributeIfAbsent(Object key, Object value) {
        return attributeMap.setAttributeIfAbsent(this, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttributeIfAbsent(Object key) {
        return setAttributeIfAbsent(key, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    public final Object removeAttribute(Object key) {
        return attributeMap.removeAttribute(this, key);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean removeAttribute(Object key, Object value) {
        return attributeMap.removeAttribute(this, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean replaceAttribute(Object key, Object oldValue, Object newValue) {
        return attributeMap.replaceAttribute(this, key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean containsAttribute(Object key) {
        return attributeMap.containsAttribute(this, key);
    }

    /**
     * {@inheritDoc}
     */
    public final Set<Object> getAttributeKeys() {
        return attributeMap.getAttributeKeys(this);
    }

    /**
     * @return The map of attributeMap associated with the session
     */
    public final IoSessionAttributeMap getAttributeMap() {
        return attributeMap;
    }

    public ServerPortType getServerPortType() {
        return serverPortType;
    }

    @Override
    public String toString() {
        return "BaseSession{" +
                "sessionId=" + sessionId +
                ", processor=" + processor +
                ", writeRequestQueue=" + writeRequestQueue +
                '}';
    }
}
