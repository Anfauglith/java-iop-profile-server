package version_01;

import com.sun.javaws.exceptions.InvalidArgumentException;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.conf.ConfigurationManager;
import version_01.configuration.ConfigurationManagerImpl;
import version_01.core.conf.ServerPortConfiguration;
import version_01.configuration.ServerPortType;
import version_01.core.service.IoHandler;
import version_01.core.service.IoProcessor;
import version_01.core.service.IoService;
import version_01.structure.internal.FilterDispatcher;
import version_01.core.filter.base.ProtocolDecoderFilter;
import version_01.core.filter.base.ProtocolEncoderFilter;
import version_01.structure.internal.NioIoProcessorManager;
import version_01.structure.internal.NioNodeAcceptorManager;
import version_01.util.ConfigurationsUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mati on 09/09/16.
 */
public class NodeServer implements IoService{

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(NodeServer.class);
    /** I/O Ports Manager */
    private NioNodeAcceptorManager nioNodeAcceptorManager;
    /** I/O Processor */
    private NioIoProcessorManager nioNodeIoProcessor;
    /** Handler */
    private Map<ServerPortType,IoHandler> handlers;
    /** Support listener (Clase usada para manejar flujos) */
    private FilterDispatcher supportIoListener;
    /** Configuration */
    private ConfigurationManager configurationManager;
    /** ServerPortTypes configurations cached */
    private Map<ServerPortType,ServerPortConfiguration> portsConfigurationMap;


    public NodeServer() throws IOException, ConfigurationException {
        handlers = new HashMap<>();
        portsConfigurationMap = new HashMap<>();
        nioNodeAcceptorManager = new NioNodeAcceptorManager(this);
        nioNodeIoProcessor = new NioIoProcessorManager(this);
        // Default event manager
        supportIoListener = new FilterDispatcher();
        configurationManager = new ConfigurationManagerImpl();
        if(!configurationManager.exist()){
            configurationManager.create();
        }else configurationManager.load();
    }

    public void start() throws Exception {

        defaultInit();

        nioNodeIoProcessor.start();

        // start nioAcceptor
        nioNodeAcceptorManager.start();

        LOG.info("Server started..");

    }

    private void defaultInit(){
        nioNodeIoProcessor.addSupportIoListener(ServerPortType.PRIMARY,supportIoListener);
        nioNodeIoProcessor.addSupportIoListener(ServerPortType.CUSTOMER,supportIoListener);
    }

    public void addServerPort(ServerPortType serverPortType){
        ServerPortConfiguration portConfiguration = ConfigurationsUtil.buildServerPortConfiguration(configurationManager,serverPortType);
        portsConfigurationMap.put(serverPortType,portConfiguration );
        nioNodeAcceptorManager.addServerPort(this,portConfiguration,serverPortType,handlers.get(serverPortType));
    }

    public void addIoProcessor(ServerPortType serverPortType,int processorsCount){
        nioNodeIoProcessor.manageServerRole(serverPortType,processorsCount);
    }

    @Override
    public IoProcessor getIoProcessor() {
        return nioNodeIoProcessor;
    }

    @Override
    public IoHandler getIoHandler(ServerPortType serverPortType) {
        return handlers.get(serverPortType);
    }

    @Override
    public ConfigurationManager getConfigurations() {
        return configurationManager;
    }

    public void registerIoHandler(ServerPortType portType, IoHandler handler) throws InvalidArgumentException {
        if (handlers.containsKey(portType)) throw new InvalidArgumentException(new String[]{"Server port type already have a handler registered"});
        handlers.put(portType,handler);
    }

    /**
     * Protocol decoder filter
     * @param protocolDecoderFilter
     */
    public void setProtocolDecoderFilter(ProtocolDecoderFilter protocolDecoderFilter){
        // seteo el protocol decoder de Strings, o sea que solo acepto strings..
        // example new StringDecoderFilter();
        supportIoListener.setProtocolDecoderFilter(protocolDecoderFilter);
    }

    /**
     * Protocol encoder filter
     * @param protocolEncoderFilter
     */
    public void setProtocolEncoderFilter(ProtocolEncoderFilter protocolEncoderFilter){
        // seteo el protocol decoder de Strings, o sea que solo acepto strings..
        supportIoListener.setProtocolEncoderFilter(protocolEncoderFilter);
    }

    @Override
    public Collection<ServerPortConfiguration> getListPortConfigurations() {
        return portsConfigurationMap.values();
    }
}
