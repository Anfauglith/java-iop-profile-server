package version_mati_01.core.session;


import version_mati_01.core.service.IoHandler;
import version_mati_01.structure.DefaultSessionConfig;

/**
 * Created by mati on 09/09/16.
 */
public interface IoSession {

    long getId();

    IoHandler getIoHandler();


    boolean isActive();

    void write(Object message) throws Exception;
}
