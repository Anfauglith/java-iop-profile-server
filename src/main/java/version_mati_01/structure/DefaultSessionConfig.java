package version_mati_01.structure;

import version_mati_01.core.session.IoSessionConfig;

/**
 * Created by mati on 11/09/16.
 */
public class DefaultSessionConfig implements IoSessionConfig{

    private int readBufferSize = 1024;

    private int maxReadBufferSize = 65536;

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

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }
}
