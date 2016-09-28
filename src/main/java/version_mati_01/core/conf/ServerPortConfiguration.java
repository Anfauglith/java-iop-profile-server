package version_mati_01.core.conf;

import version_mati_01.configuration.ServerPortType;

/**
 * Created by mati on 09/09/16.
 */
public interface ServerPortConfiguration {

    ServerPortType getPortType();

    int getPort();

    boolean isReuseAddress();

    boolean isSecure();

    int getBacklog();

    int getIoThreadsCount();

    int getWorkersCount();
}
