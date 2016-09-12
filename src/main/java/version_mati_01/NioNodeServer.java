package version_mati_01;

import version_mati_01.configuration.NodeConfiguration;
import version_mati_01.configuration.NodeConfigurationImpl;
import version_mati_01.core.filter.IoFilterChainBuilder;
import version_mati_01.core.service.IoHandler;
import version_mati_01.core.service.IoProcessor;
import version_mati_01.core.service.IoService;
import version_mati_01.structure.DefaultSupportIoListener;
import version_mati_01.structure.filters.StringDecoderFilter;
import version_mati_01.structure.filters.StringEncoderFilter;

/**
 * Created by mati on 09/09/16.
 */
public class NioNodeServer implements IoService{

    /** I/O Acceptor Processor */
    private NioNodeAcceptor nioNodeAcceptor;
    /** I/O Processor */
    private NioIoProcessorManager nioNodeIoProcessor;

    private NodeConfiguration nodeConfiguration;

    private IoHandler ioHandler;
    /** Filter chain builder */
    private IoFilterChainBuilder filterChainBuilder;

    private DefaultSupportIoListener supportIoListener;


    public NioNodeServer() {
        nioNodeAcceptor = new NioNodeAcceptor(this);
        nioNodeIoProcessor = new NioIoProcessorManager(this,1);
    }

    private void defaultInit(){

        // Default event manager
        supportIoListener = new DefaultSupportIoListener();
        // seteo el protocol decoder de Strings, o sea que solo acepto strings..
        supportIoListener.setProtocolDecoderFilter(new StringDecoderFilter());
        // seteo el protocol encoder de String, o se que envio solo Strings..
        supportIoListener.setProtocolEncoderFilter(new StringEncoderFilter());
        //init filterChainBuilder
//        filterChainBuilder = new DefaultFilterChainBuilder();
        nioNodeIoProcessor.setSupportIoListener(supportIoListener);

        if (nodeConfiguration==null){
            nodeConfiguration = NodeConfigurationImpl.getInstance();
        }
        nioNodeAcceptor.setNodeConfiguration(nodeConfiguration);
        nioNodeAcceptor.setIoHandler(ioHandler);
    }

    public void start() throws Exception {

        defaultInit();

        nioNodeIoProcessor.start();

        // start nioAcceptor
        nioNodeAcceptor.start();


    }

    @Override
    public IoProcessor getIoProcessor() {
        return nioNodeIoProcessor;
    }

    @Override
    public IoFilterChainBuilder getFilterChainBuilder() {
        return filterChainBuilder;
    }

    public void setNodeConfiguration(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
    }

    public void setIoHandler(IoHandler ioHandler){
        this.ioHandler = ioHandler;
    }
}
