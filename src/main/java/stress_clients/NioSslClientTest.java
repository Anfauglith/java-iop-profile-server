package stress_clients;

import version_mati_01.ssl.SslContextFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mati on 19/09/16.
 */
public class NioSslClientTest extends Thread{

    private SSLSocket sslSocket;

    private static int clientsOk;

    int clientId;

    public NioSslClientTest(int id,SSLContext sslContext, String host, int port) throws IOException {
        this.sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(host,port);
        this.sslSocket.setReuseAddress(true);
        this.clientId = id;
    }

    public void run(){
        try {

            System.out.println("Running..");
            int count;
            byte[] buffer = new byte[8192];
            // send request
            sslSocket.getOutputStream().write(("hello: "+clientId).getBytes());

            System.out.println("prev first handshake, id: "+clientId);
            // handshake before read
            sslSocket.startHandshake();
            // read reply
            count = sslSocket.getInputStream().read(buffer);
            System.out.println("client: "+clientId+" (1) got "+ new String(buffer,0,count));
            // get a new session & do a full handshake
            sslSocket.getSession().invalidate();
            sslSocket.startHandshake();
            // send another request
            sslSocket.getOutputStream().write(("Hello again after new handshake ,id: " +clientId).getBytes());
            // Do a partial handshake before reading the reply
            sslSocket.startHandshake();
            // read reply
            count = sslSocket.getInputStream().read(buffer);
            System.out.println("client: "+clientId+" (2) got "+ new String(buffer,0,count));

        }catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                clientsOk++;
                System.out.println("Clientes atendidos bien hasta el momento: "+ clientsOk);
                sslSocket.close();
                System.out.println("client: socket closed, id: "+clientId);
            }catch (IOException e){
//                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){

        try {

            int clientsCont = 10000;

            SSLContext sslContext = SslContextFactory.buildContext();
            ExecutorService executorService = Executors.newFixedThreadPool(clientsCont);

            for (int i=0;i<clientsCont;i++){
                int finalI = i;
                executorService.submit(() -> {
                    try {
                        new NioSslClientTest(finalI +1,sslContext,"localhost",9888).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
