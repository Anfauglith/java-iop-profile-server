package version_mati_01.core.service;


import version_mati_01.core.conf.ConfigurationManager;
import version_mati_01.configuration.ServerPortType;
import version_mati_01.core.conf.ServerPortConfiguration;

import java.util.Collection;

/**
 * Created by mati on 09/09/16.
 */
public interface IoService {

    IoProcessor getIoProcessor();

    IoHandler getIoHandler(ServerPortType serverPortType);

    ConfigurationManager getConfigurations();

    Collection<ServerPortConfiguration> getListPortConfigurations();

}
