package version_01.structure.filters.ssl.test;

import com.sun.javaws.exceptions.InvalidArgumentException;
import org.apache.commons.configuration.ConfigurationException;
import version_01.NodeServer;
import version_01.configuration.ServerPortType;
import version_01.core.service.IoHandler;
import version_01.core.session.IoSession;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Created by mati on 13/09/16.
 */
public class SslExample {


    public static void main(String[] args){

        String hostname = "localhost";
        int port = 9888;

        char[] passphrase = "passphrase".toCharArray();

        try {

            //Create SslContext
            SSLContext sslContext = SslContextFactory.createSslContext(passphrase);

            // We're ready for the engine.
            SSLEngine engine = sslContext.createSSLEngine(hostname, port);
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(true);




            // Use as client
//            engine.setUseClientMode(true);
            NodeServer nioNodeServer = new NodeServer();
            // handler
            try {
                nioNodeServer.registerIoHandler(ServerPortType.CUSTOMER,new IoHandler() {
                    @Override
                    public void sessionCreated(IoSession session) throws Exception {
                        System.out.println("Creé una nueva sesion!!");
                    }

                    @Override
                    public void sessionOpened(IoSession session) throws Exception {

                    }

                    @Override
                    public void sessionClosed(IoSession session) throws Exception {
                        System.out.println("Cerré una sesion!!");
                    }

                    @Override
                    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                        cause.printStackTrace();
                    }

                    @Override
                    public void messageReceived(IoSession session, Object message) throws Exception {
                        System.out.println("Mensaje llegó: " + message);
                        System.out.println("Ahora voy a enviar uno..");
    //                    String messageToReplay = "todo ok!";
    //                    session.write(messageToReplay);
                    }

                    @Override
                    public void messageSent(IoSession session, Object message) throws Exception {
                        System.out.println("Mandé un mensaje: " + message);
                    }

                    @Override
                    public void inputClosed(IoSession session) throws Exception {

                    }
                });
            } catch (InvalidArgumentException e) {
                e.printStackTrace();
            }

            try {
                nioNodeServer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }


    }

}
