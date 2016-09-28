package version_01.structure.session;

import version_01.core.session.IoSessionConfig;

/**
 * Created by mati on 11/09/16.
 */
public class DefaultSessionConfig implements IoSessionConfig{

    private int readBufferSize = 1024;

    private int maxReadBufferSize = 65536;

    private int writeBufferSize = 1024;

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
}
