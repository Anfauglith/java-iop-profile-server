package version_01.core.service;


import version_01.core.listener.SupportIoListener;
import version_01.core.session.IoSession;
import version_01.core.write.WriteRequest;
import version_01.structure.session.BaseSession;

/**
 * An internal interface to represent an 'I/O processor' that performs
 * actual I/O operations for {@link IoSession}s.  It abstracts existing
 * reactor frameworks such as Java NIO once again to simplify transport
 * implementations.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 * @param <S> the type of the {@link IoSession} this processor can handle
 */
public interface IoProcessor<S extends IoSession> {

    /**
     * @return <tt>true</tt> if and if only {@link #dispose()} method has
     * been called.  Please note that this method will return <tt>true</tt>
     * even after all the related resources are released.
     */
    boolean isDisposing();

    /**
     * @return <tt>true</tt> if and if only all resources of this processor
     * have been disposed.
     */
    boolean isDisposed();

    /**
     * Releases any resources allocated by this processor.  Please note that
     * the resources might not be released as long as there are any sessions
     * managed by this processor.  Most implementations will close all sessions
     * immediately and release the related resources.
     */
    void dispose() throws Exception;

    /**
     * Adds the specified {@code session} to the I/O processor so that
     * the I/O processor starts to perform any I/O operations related
     * with the {@code session}.
     *
     * @param session The added session
     */
    void add(S session);

    /**
     * Flushes the internal write request queue of the specified
     * {@code session}.
     *
     * @param session The session we want the message to be written
     */
    void flush(S session);

    /**
     * Writes the WriteRequest for the specified {@code session}.
     *
     * @param session The session we want the message to be written
     * @param writeRequest the WriteRequest to write
     */
    void write(BaseSession session, WriteRequest writeRequest);

    /**
     * Controls the traffic of the specified {@code session} depending of the
     * {@link IoSession#isReadSuspended()} and {@link IoSession#isWriteSuspended()}
     * flags
     *
     * @param session The session to be updated
     */
    void updateTrafficControl(S session);

    /**
     * Removes and closes the specified {@code session} from the I/O
     * processor so that the I/O processor closes the connection
     * associated with the {@code session} and releases any other related
     * resources.
     *
     * @param session The session to be removed
     */
    void remove(S session);


    void setSupportIoListener(SupportIoListener supportIoListener);

    SupportIoListener getSupportIoListener();

    Runnable startupProcessorRunnable();

    void scheduleFlush(S session);

}
