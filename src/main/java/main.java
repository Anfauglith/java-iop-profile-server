import version_01.NodeServer;
import version_01.configuration.ServerPortType;
import version_01.structure.filters.protocol.ProtobufDecoderFilter;
import version_01.structure.filters.protocol.ProtobufEncoderFilter;
import version_01.structure.handlers.CPHandler;
import version_01.structure.handlers.NCPHandler;
import version_01.structure.handlers.PPHandler;

/**
 * Created by mati on 08/09/16.
 */
public class main {


    public static void main(String[] args){

        try {

            NodeServer nioNodeServer = new NodeServer();
            // Protocol encoder
            nioNodeServer.setProtocolEncoderFilter(new ProtobufEncoderFilter());
            // Protocol decoder
            nioNodeServer.setProtocolDecoderFilter(new ProtobufDecoderFilter());
            // Primary handler
            nioNodeServer.registerIoHandler(ServerPortType.PRIMARY, new PPHandler(nioNodeServer));
            // Non-customer handler
            nioNodeServer.registerIoHandler(ServerPortType.NON_CUSTOMER, new NCPHandler());
            // Customer handler
            nioNodeServer.registerIoHandler(ServerPortType.CUSTOMER, new CPHandler());
            // Primary port
            nioNodeServer.addServerPort(ServerPortType.PRIMARY);
            // Non-customer port
            nioNodeServer.addServerPort(ServerPortType.NON_CUSTOMER);
            // Customer port
            nioNodeServer.addServerPort(ServerPortType.CUSTOMER);
            // Primary Processors
            nioNodeServer.addIoProcessor(ServerPortType.PRIMARY, 2);
            // Non-customer Processor
            nioNodeServer.addIoProcessor(ServerPortType.NON_CUSTOMER, 2);
            // Customer Processor
            nioNodeServer.addIoProcessor(ServerPortType.CUSTOMER, Runtime.getRuntime().availableProcessors());
            // Start
            nioNodeServer.start();

        }catch (Exception e){
            e.printStackTrace();
        }

    }


}
