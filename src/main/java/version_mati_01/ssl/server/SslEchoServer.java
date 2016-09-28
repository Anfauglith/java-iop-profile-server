package version_mati_01.ssl.server;

import version_mati_01.ssl.SslContextFactory;
import version_mati_01.ssl.SslManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by mati on 16/09/16.
 */
public class SslEchoServer {

    SSLContext sslContext;
    ServerSocketChannel serverSocketChannel;
    Selector selector;

    public SslEchoServer() throws Exception{

        this.sslContext = SSLContext.getInstance("TLS");

//        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//        KeyStore keyStore = KeyStore.getInstance("JKS");
//        char[] password = System.getProperty("javax.net.ssl.KeyStorePassword").toCharArray();
//        keyStore.load(new FileInputStream(System.getProperty("javax.net.ssl.KeyStore")),password);
//        kmf.init(keyStore,password);
//        sslContext.init(kmf.getKeyManagers(),null,null);

        sslContext = SslContextFactory.buildContext();

        // start the server
        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().setReuseAddress(true);
        serverSocketChannel.socket().bind(new InetSocketAddress(15000));
        System.out.println("Server: listening at "+serverSocketChannel);
        this.selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

    }


    public void run(){


        int count;

        while (selector.keys().size()>0){
            try{
                count = selector.select(30*100);
                if (count<0){
                    System.out.println("Server: select timeout");
                    continue;
                }

                System.out.println("Server: select count= "+count);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                while (it.hasNext()){
                    SelectionKey selectionKey = it.next();
                    it.remove();
                    if (!selectionKey.isValid()){
                        continue;
                    }
                    try {
                        if (selectionKey.isAcceptable()){
                            handleAccept(selectionKey);
                        }
                        if (selectionKey.isReadable()){
                            handleRead(selectionKey);
                        }
                        if (selectionKey.isWritable()){
                            handleWrite(selectionKey);
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }


                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRead(SelectionKey selectionKey) throws IOException{
        SslManager sslManager = (SslManager) selectionKey.attachment();
        SSLEngine sslEngine = sslManager.getEngine();
        ByteBuffer request = sslManager.getAppInpBuffer();
        System.out.println("Server: reading..");
        int count = sslManager.read();
        System.out.println("Server: read count: "+count+" request: "+request);
        if (count<0){
            // client has closed
            sslManager.close();
            // finished with this key
            selectionKey.cancel();
            // finished with this test actually
            serverSocketChannel.close();
        } else
            if (request.position() > 0){
                // client request
                System.out.println("Server: read"+ new String(request.array(),0,request.position()));
                ByteBuffer reply = sslManager.getAppOutBuffer();
                request.flip();
                reply.put(request);
                request.compact();
                handleWrite(selectionKey);
            }

        System.out.println("handshake status: "+ sslManager.getEngine().getHandshakeStatus());

    }

    private void handleWrite(SelectionKey selectionKey) throws IOException{
        SslManager sslManager = (SslManager) selectionKey.attachment();
        ByteBuffer reply = sslManager.getAppOutBuffer();
        System.out.println("Server: writing "+reply);
        int count = 0;
        while (reply.position() > 0){
//            reply.flip();
            count = sslManager.write();
//            reply.compact();
            if (count == 0){
                break;
            }
        }
        if (reply.position() > 0){
            // short write
            // Register for OP_WRITE and come bacj here when ready
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        } else {
            // Write succeded, don't need OP_WRITE any more
            selectionKey.interestOps(selectionKey.interestOps() &~ SelectionKey.OP_WRITE);
        }

    }

    private void handleAccept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        // create engine
        SSLEngine engine = sslContext.createSSLEngine("localhost",socketChannel.socket().getPort());
        // this is the server end
        engine.setUseClientMode(false);
        engine.setNeedClientAuth(true);
        engine.setWantClientAuth(true);
        // Create engine manager for the channel & engine
        SslManager sslManager = new SslManager(socketChannel,engine);
        // register and attach the sslManager
        socketChannel.register(selector,SelectionKey.OP_READ,sslManager);
    }


    public static void main(String[] args){
        System.setProperty("javax.net.ssl.keyStore","testkeys");
        System.setProperty("javax.net.ssl.keyStorePassword","passphrase");

        try {
            new SslEchoServer().run();
            System.out.println("Exiting..");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
