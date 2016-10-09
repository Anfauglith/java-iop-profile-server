import version_01.NodeServer;
import version_01.configuration.ServerPortType;
import version_01.home_net.data.db.DatabaseFactory;
import version_01.structure.filters.protocol.ProtobufDecoderFilter;
import version_01.structure.filters.protocol.ProtobufEncoderFilter;
import version_01.structure.logic.crypto.CryptoImp;
import version_01.structure.logic.handlers.CPHandler;
import version_01.structure.logic.handlers.NCPHandler;
import version_01.structure.logic.handlers.PPHandler;
import version_01.util.ClassScope;
import version_01.util.DirResourcesFilesPathUtil;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * Created by mati on 08/09/16.
 */
public class Main {

//    static {
//
////        System.setProperty("java.library.path", "/home/mati/Documentos/apache-mina/node/libs/armeabi");
//
//        String property = System.getProperty("java.library.path");
//        StringTokenizer parser = new StringTokenizer(property, ";");
//        while (parser.hasMoreTokens()) {
//            System.err.println(parser.nextToken());
//        }
//        System.out.println("------------------------------");
//
//        System.loadLibrary("kaliumjni");
//
//    }

    public static void main(String[] args){

        try {

            NodeServer nioNodeServer = new NodeServer();
            // Create resources external folder
            DirResourcesFilesPathUtil.createNewFilesPath("");
            // Database
            DatabaseFactory databaseFactory = new DatabaseFactory(DirResourcesFilesPathUtil.getExternalStoregaDirectory("database"));
            nioNodeServer.setDatabaseFactory(databaseFactory);
            // Crypto
            CryptoImp cryptoImp = new CryptoImp();
            // Protocol encoder
            nioNodeServer.setProtocolEncoderFilter(new ProtobufEncoderFilter());
            // Protocol decoder
            nioNodeServer.setProtocolDecoderFilter(new ProtobufDecoderFilter());
            // Primary handler
            nioNodeServer.registerIoHandler(ServerPortType.PRIMARY, new PPHandler(nioNodeServer));
            // Non-customer handler
            nioNodeServer.registerIoHandler(ServerPortType.NON_CUSTOMER, new NCPHandler(databaseFactory,cryptoImp));
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
