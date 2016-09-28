package version_mati_01.core.session;

/**
 * Created by mati on 10/09/16.
 */
public interface IoSessionConfig {

    int getReadBufferSize();

    int getMaxReadBufferSize();

    int getWriteBufferSize();
}
