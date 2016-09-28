package version_01.ssl.client;

import version_01.ssl.SslContextFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;

/**
 * Created by mati on 16/09/16.
 */
public class SslEchoClient extends Thread {

    private SSLSocket sslSocket;

    public SslEchoClient(SSLContext sslContext, String host, int port) throws IOException {
        this.sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(host,port);
        this.sslSocket.setReuseAddress(true);
    }

    public void run(){
        try {

            System.out.println("Running..");
            int count;
            byte[] buffer = new byte[8192];
            // send request
            sslSocket.getOutputStream().write("hello".getBytes());

            System.out.println("prev first handshake");
            // handshake before read
            sslSocket.startHandshake();
            // read reply
            count = sslSocket.getInputStream().read(buffer);
            System.out.println("client: (1) got "+ new String(buffer,0,count));
            // get a new session & do a full handshake
            sslSocket.getSession().invalidate();
            sslSocket.startHandshake();
            // send another request
            sslSocket.getOutputStream().write("Hello again after new handshake".getBytes());
            // Do a partial handshake before reading the reply
            sslSocket.startHandshake();
            // read reply
            count = sslSocket.getInputStream().read(buffer);
            System.out.println("client: (2) got "+ new String(buffer,0,count));

        }catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                sslSocket.close();
                System.out.println("client: socket closed");
            }catch (IOException e){
//                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        SSLContext sslContext = null;
        try {
            sslContext = SslContextFactory.buildContext();
            new SslEchoClient(sslContext,"localhost",9888).start();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
