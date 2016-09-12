package version_mati_01;

import version_mati_01.configuration.NodeConfiguration;
import version_mati_01.core.service.IoHandler;
import version_mati_01.core.service.IoService;
import version_mati_01.core.session.IoSession;
import version_mati_01.structure.ClientSession;
import version_mati_01.structure.DefaultSessionConfig;
import version_mati_01.structure.DefaultWriteRequestQueu;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mati on 08/09/16.
 */
public class NioNodeAcceptor {

    private ServerSocketChannel serverSocketChannel;

    private volatile Selector selector;

    private NodeConfiguration nodeConfiguration;

    private ExecutorService executorService;

    private IoHandler ioHandler;

    /** Server service class */
    private IoService ioService;
    /** flag to know is the server is on */
    private boolean selectable;
    /** number of acceptor threads, default=1 */
    private int threadsCount = 1;

    public NioNodeAcceptor(IoService ioService) {
        this.ioService = ioService;
    }

    public NioNodeAcceptor(IoService ioService, int threadsCount) {
        this.ioService = ioService;
        this.threadsCount = threadsCount;
    }

    public void start() throws Exception{

        /**
         * Open ServerSocketChannel and configure it
         */
        selectable = open();

        /**
         * startAcceptor
         */
        startAcceptor();

    }

    /**
     *  Open and configure the channel
     *
     * @return
     * @throws Exception
     */
    private boolean open() throws Exception{
        //open channel and configuration
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        // flag to know if the server is fine
        boolean success = false;

        try {
            //configure the server socket
            ServerSocket serverSocket = serverSocketChannel.socket();

            // Set the reuse address flag
            serverSocket.setReuseAddress(nodeConfiguration.isReuseAddress());

            // bind
            InetSocketAddress localAddress = new InetSocketAddress(nodeConfiguration.getPort());
            //todo: acá le puedo poner el backlog, fijarse si me hace falta
            try {
                serverSocket.bind(localAddress, nodeConfiguration.getBacklog());
            } catch (IOException ex) {
                String newMessage = "Error while binding on " + localAddress + "\n" + "original message : "
                        + ex.getMessage();
                Exception e = new IOException(newMessage);
                e.initCause(ex.getCause());

                // And close the channel
                serverSocketChannel.close();

                throw e;
            }

            // open selector
            selector = Selector.open();
            // register the channel within the selector for ACCEPT event
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            success = true;
        }finally {
            if (!success){
                serverSocketChannel.close();
            }
        }
        return success;
    }

    private void startAcceptor() {

        ThreadFactory threadFactory = new ThreadFactory() {

            private AtomicInteger threadNumbers = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Acceptor-Processor-Thread-"+threadNumbers.incrementAndGet());
                return thread;
            }
        };

        // create executor based on threadCount
        executorService = Executors.newFixedThreadPool(threadsCount,threadFactory);
        // run acceptor processor
        executorService.submit(new Acceptor());
    }


    public void setNodeConfiguration(NodeConfiguration nodeConfiguration){
        this.nodeConfiguration = nodeConfiguration;
    }

    public void setIoHandler(IoHandler ioHandler){
        this.ioHandler = ioHandler;
    }

    private int select() throws IOException {
        return selector.select();
    }

    private Iterator<SelectionKey> selectedKeys(){
        return selector.selectedKeys().iterator();
    }

    /**
     *  This method will process new sessions for the worker acceptor class.
     * Solo las keys con OP_ACCEPT serán procesadas acá.
     * <p/>
     * Los objetos de session son creados haciendo una nueva instancia de SocketSession
     * y pasando el objeto de sesion al SocketIoProcesor.
     *
     * @param it
     */
    private void processKeys(Iterator<SelectionKey> it){
        while (it.hasNext()){

            try {
                SelectionKey selectionKey = it.next();
                it.remove();

                SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();

                // here i can configure the socketChannel
                //do something..

                // creo la session con configuracion por default y la cola de escritos por default
                ClientSession s = ClientSession.clientSessionFactory(socketChannel,ioHandler,new DefaultSessionConfig(),new DefaultWriteRequestQueu());

                // seteo el procesador para la session
                s.setProcessor(ioService.getIoProcessor());

                // inicializo la session creada
                initSession(s);

                // add session to the waiting new session queue
                ioService.getIoProcessor().add(s);

//                try {
//                    // fire session create event
//                    s.getIoHandler().sessionCreated(s);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void initSession(IoSession s) {
        // init session with parameters like time, etc..

    }

    /**
     * Implements a infite loop accepting connections
     * this loop will stop when server is shutdown
     */
    private class Acceptor implements Runnable{

        public void run() {

            while (selectable){

                try {
                    int selectedKeys = select();

                    if (selectedKeys>0){

                        Iterator<SelectionKey> it = selectedKeys();

                        processKeys(it);

                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }




        }
    }

}
