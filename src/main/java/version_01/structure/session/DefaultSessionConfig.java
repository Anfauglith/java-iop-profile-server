package version_01.structure.session;

import version_01.core.session.IdleStatus;
import version_01.core.session.IoSessionConfig;

/**
 * Created by mati on 11/09/16.
 */
public class DefaultSessionConfig implements IoSessionConfig{

    private int readBufferSize = 1024;

    private int maxReadBufferSize = 65536;

    private int writeBufferSize = 1024;

    /** Socket timeout in millis, 1 min -> 60.000 milliseconds */
    private int socketTimeout = 60000;

    private int idleTime = 60000;

    // todo: ver que cosa es esto, si son bytes, megabytes o que carajo.
    private int socketReadBufferSize = 65536;

    public DefaultSessionConfig() {

    }

    @Override
    public int getReadBufferSize() {
        return readBufferSize;
    }

    @Override
    public int getMaxReadBufferSize() {
        return maxReadBufferSize;
    }

    @Override
    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    @Override
    public int getIdleTime(IdleStatus status) {
        return idleTime;
    }

    @Override
    public long getIdleTimeInMillis(IdleStatus idleStatus) {
        return idleTime;
    }

    @Override
    public long getWriteTimeoutInMillis() {
        return idleTime;
    }

    @Override
    public int getSocketReadBufferSize() {
        return socketReadBufferSize;
    }
}
