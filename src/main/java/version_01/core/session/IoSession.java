package version_01.core.session;


import version_01.core.service.IoHandler;
import version_01.core.write.WriteRequest;
import version_01.core.write.WriteRequestQueue;

import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * Created by mati on 09/09/16.
 */
public interface IoSession {

    long getId();

    IoHandler getIoHandler();


    boolean isActive();

    void write(Object message) throws Exception;

    void close();

    boolean setScheduledForFlush(boolean scheduleForFlush);

    void unscheduledForFlush();

    WriteRequestQueue getWriteRequestQueue();

    boolean isScheduledForFlush();

    void closeNow();

    SelectableChannel getChannel();

    void setSelectionKey(SelectionKey key);

    boolean containsAttribute(Object value);

    InetSocketAddress getLocalAddress();

    void setSecure(boolean isSecure);

    Object setAttribute(Object key, Object value);

    boolean isSecure();

    SelectionKey getSelectionKey();

    boolean isReadSuspended();

    boolean isWriteSuspended();

    Object getAttribute(Object key);

    void addWriteRequest(WriteRequest writeRequest);
}
