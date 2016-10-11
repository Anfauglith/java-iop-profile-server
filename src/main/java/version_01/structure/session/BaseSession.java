package version_01.structure.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.configuration.ServerPortType;
import version_01.core.listener.SupportIoListener;
import version_01.core.service.IoHandler;
import version_01.core.service.IoProcessor;
import version_01.core.session.IdleStatus;
import version_01.core.session.IoSession;
import version_01.core.session.IoSessionAttributeMap;
import version_01.core.session.IoSessionConfig;
import version_01.core.write.SessionCloseException;
import version_01.core.write.WriteRequest;
import version_01.core.write.WriteTimeoutException;
import version_01.util.SessionUtil;
import version_01.core.write.WriteRequestQueue;


import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    private AtomicBoolean conversationStarted = new AtomicBoolean(false);

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

    private long creationTime;
    private long lastReadTime;
    private long lastWriteTime;

    /** Idle counters */
    private AtomicInteger idleCountForBoth = new AtomicInteger(0);
    private AtomicInteger idleCountForRead = new AtomicInteger(0);
    private AtomicInteger idleCountForWrite = new AtomicInteger(0);
    /** Idle time */
    private long lastIdleTimeForBoth;
    private long lastIdleTimeForRead;
    private long lastIdleTimeForWrite;



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

    @Override
    public boolean isConversationStarted() {
        return conversationStarted.get();
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


    public void setConversationStarted(boolean isConcersationStarted) {
        this.conversationStarted.set(isConcersationStarted);
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

    /**
     * in the specified collection.
     *
     * @param sessions The sessions that are notified
     * @param currentTime the current time (i.e. {@link System#currentTimeMillis()})
     */
    public static void notifyIdleness(Iterator<? extends IoSession> sessions, long currentTime,SupportIoListener supportIoListener) {
        while (sessions.hasNext()) {
            IoSession session = sessions.next();

            if (session.isActive()) {
                //notifyIdleSession(session, currentTime,supportIoListener);
                checkIdleSession(session,currentTime,supportIoListener);
            }
        }
    }

    private static void checkIdleSession(IoSession session, long currentTime, SupportIoListener supportIoListener) {
        if (session.hasTobeClosed(currentTime)){
            // close session
            session.closeNow();
        }
    }

    /**
     * Fires a {@link} event if applicable for the
     * specified {@code session}.
     *
     * @param session The session that is notified
     * @param currentTime the current time (i.e. {@link System#currentTimeMillis()})
     */
    public static void notifyIdleSession(IoSession session, long currentTime, SupportIoListener supportIoListener) {
        notifyIdleSession0(session, currentTime, session.getConfig().getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                IdleStatus.BOTH_IDLE, Math.max(session.getLastIoTime(), session.getLastIdleTime(IdleStatus.BOTH_IDLE)),supportIoListener);

        notifyIdleSession0(session, currentTime, session.getConfig().getIdleTimeInMillis(IdleStatus.READER_IDLE),
                IdleStatus.READER_IDLE,
                Math.max(session.getLastReadTime(), session.getLastIdleTime(IdleStatus.READER_IDLE)),supportIoListener);

        notifyIdleSession0(session, currentTime, session.getConfig().getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                IdleStatus.WRITER_IDLE,
                Math.max(session.getLastWriteTime(), session.getLastIdleTime(IdleStatus.WRITER_IDLE)),supportIoListener);

        notifyWriteTimeout(session, currentTime,supportIoListener);
    }

    private static void notifyIdleSession0(IoSession session, long currentTime, long idleTime, IdleStatus status, long lastIoTime,SupportIoListener supportIoListener) {
        if ((idleTime > 0) && (lastIoTime != 0) && (currentTime - lastIoTime >= idleTime)) {
            supportIoListener.fireSessionIdle(session,status);
        }
    }

    private static void notifyWriteTimeout(IoSession session, long currentTime,SupportIoListener supportIoListener) {

        long writeTimeout = session.getConfig().getWriteTimeoutInMillis();
        if ((writeTimeout > 0) && (currentTime - session.getLastWriteTime() >= writeTimeout)
                && !session.getWriteRequestQueue().isEmpty()) {
            WriteRequest request = session.getCurrentWriteRequest();
            if (request != null) {
                session.setCurrentWriteRequest(null);
                WriteTimeoutException cause = new WriteTimeoutException("WriteRequest: "+request.toString());
                request.getFuture().setException(cause);
                supportIoListener.fireExceptionCaught(session,"",cause);
                // WriteException is an IOException, so we close the session.
                session.close();
            }
        }
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


    /**
     * {@inheritDoc}
     */
    public final long getCreationTime() {
        return creationTime;
    }
    /**
     * {@inheritDoc}
     */
    public final long getLastIoTime() {
        return Math.max(lastReadTime, lastWriteTime);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastReadTime() {
        return lastReadTime;
    }

    /**
     * Check if this session has to be closed
     *
     * @param currentTime
     * @return
     */
    public final boolean hasTobeClosed(long currentTime){
        return  (!isScheduledForFlush() && currentTime-lastWriteTime>config.getIdleTimeInMillis(IdleStatus.WRITER_IDLE) && currentTime-lastReadTime>config.getIdleTimeInMillis(IdleStatus.READER_IDLE));
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastWriteTime() {
        return lastWriteTime;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isIdle(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleCountForBoth.get() > 0;
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleCountForRead.get() > 0;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleCountForWrite.get() > 0;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }


    /**
     * {@inheritDoc}
     */
    public final boolean isBothIdle() {
        return isIdle(IdleStatus.BOTH_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isReaderIdle() {
        return isIdle(IdleStatus.READER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isWriterIdle() {
        return isIdle(IdleStatus.WRITER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final int getIdleCount(IdleStatus status) {
        if (getConfig().getIdleTime(status) == 0) {
            if (status == IdleStatus.BOTH_IDLE) {
                idleCountForBoth.set(0);
            }

            if (status == IdleStatus.READER_IDLE) {
                idleCountForRead.set(0);
            }

            if (status == IdleStatus.WRITER_IDLE) {
                idleCountForWrite.set(0);
            }
        }

        if (status == IdleStatus.BOTH_IDLE) {
            return idleCountForBoth.get();
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleCountForRead.get();
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleCountForWrite.get();
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastIdleTime(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return lastIdleTimeForBoth;
        }

        if (status == IdleStatus.READER_IDLE) {
            return lastIdleTimeForRead;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return lastIdleTimeForWrite;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    /**
     * Increase the count of the various Idle counter
     *
     * @param status The current status
     * @param currentTime The current time
     */
    public final void increaseIdleCount(IdleStatus status, long currentTime) {
        if (status == IdleStatus.BOTH_IDLE) {
            idleCountForBoth.incrementAndGet();
            lastIdleTimeForBoth = currentTime;
        } else if (status == IdleStatus.READER_IDLE) {
            idleCountForRead.incrementAndGet();
            lastIdleTimeForRead = currentTime;
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleCountForWrite.incrementAndGet();
            lastIdleTimeForWrite = currentTime;
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
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
