package version_01.structure.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.conf.ConfigurationManager;
import version_01.core.conf.ServerPortConfiguration;
import version_01.configuration.ServerPortType;
import version_01.core.service.IoHandler;
import version_01.core.service.IoProcessor;
import version_01.core.service.IoService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mati on 22/09/16.
 *
 * Server port manager class
 */
public class NioNodeAcceptorManager implements IoService{

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(NioNodeAcceptorManager.class);
    /** base server class */
    private IoService ioService;
    /** server port listening */
    private Map<ServerPortType, NioNodeAcceptor> serversPort;

    public NioNodeAcceptorManager(IoService ioService) {
        this.ioService = ioService;
        serversPort = new HashMap<>();
    }

    public NioNodeAcceptorManager addServerPort(IoService ioService,ServerPortConfiguration serverPortConfiguration,ServerPortType serverPortType,IoHandler ioHandler){
        serversPort.put(serverPortType,new NioNodeAcceptor(ioService,serverPortConfiguration,serverPortType,ioHandler));
        return this;
    }


    @Override
    public IoProcessor getIoProcessor() {
        return ioService.getIoProcessor();
    }

    @Override
    public IoHandler getIoHandler(ServerPortType portType) {
        return ioService.getIoHandler(portType);
    }

    @Override
    public ConfigurationManager getConfigurations() {
        return ioService.getConfigurations();
    }

    @Override
    public Collection<ServerPortConfiguration> getListPortConfigurations() {
        return ioService.getListPortConfigurations();
    }


    public void start() {
        LOG.info("Starting acceptors...");
        serversPort.forEach((serverPortType, nioNodeAcceptor) -> {
            try {
                nioNodeAcceptor.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
