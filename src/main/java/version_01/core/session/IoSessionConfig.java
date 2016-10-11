package version_01.core.session;

/**
 * Created by mati on 10/09/16.
 */
public interface IoSessionConfig {

    int getReadBufferSize();

    int getMaxReadBufferSize();

    int getWriteBufferSize();

    /**
     * Get socket timeout in millis
     * @return
     */
    int getSocketTimeout();

    int getIdleTime(IdleStatus status);

    long getIdleTimeInMillis(IdleStatus idleStatus);

    long getWriteTimeoutInMillis();

    int getSocketReadBufferSize();
}
