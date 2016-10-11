package version_01.structure.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.conf.ServerPortConfiguration;
import version_01.configuration.ServerPortType;
import version_01.core.service.IoHandler;
import version_01.core.service.IoService;
import version_01.core.session.IoSessionConfig;
import version_01.structure.session.BaseSession;
import version_01.structure.session.DefaultSessionConfig;
import version_01.structure.session.DefaultWriteRequestQueu;
import version_01.structure.session.DefaultIoSessionAttributeMap;

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

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(NioNodeAcceptor.class);

    /** Server */
    private ServerSocketChannel serverSocketChannel;
    /** Server selector */
    private volatile Selector selector;
    /**  */
    private ExecutorService executorService;
    /**  */
    private IoHandler ioHandler;
    /** Server service class */
    private IoService ioService;
    /** flag to know is the server is on */
    private boolean selectable;
    /** number of acceptor threads, default=1 */
    private int threadsCount;
    /** Port */
    private ServerPortConfiguration serverPortConfiguration;
    /** Type */
    private ServerPortType serverPortType;

    /** Default session config for every channel in this acceptor*/
    private IoSessionConfig sessionConfig;

    public NioNodeAcceptor(IoService ioService, ServerPortConfiguration serverPortConfiguration, ServerPortType serverPortType,IoHandler ioHandler) {
        this(ioService,serverPortConfiguration.getIoThreadsCount(),serverPortConfiguration,serverPortType,ioHandler);
        this.sessionConfig = new DefaultSessionConfig();
    }

    public NioNodeAcceptor(IoService ioService, int threadsCount,ServerPortConfiguration serverPortConfiguration,ServerPortType serverPortType,IoHandler ioHandler) {
        this.ioService = ioService;
        this.threadsCount = threadsCount;
        this.serverPortConfiguration = serverPortConfiguration;
        this.serverPortType = serverPortType;
        this.ioHandler = ioHandler;
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

        /** Log to inform situation */
        LOG.info("Acceptor "+serverPortType+" on port: "+serverPortConfiguration.getPort()+" started "+ serverSocketChannel.getLocalAddress()+"\nAcceptor thread count: "+threadsCount);

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
            serverSocket.setReuseAddress(serverPortConfiguration.isReuseAddress());

            // bind
            InetSocketAddress localAddress = new InetSocketAddress(serverPortConfiguration.getPort());
            //todo: acá le puedo poner el backlog, fijarse si me hace falta
            try {
                serverSocket.bind(localAddress, serverPortConfiguration.getBacklog());
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


    private int select() throws IOException {
        return selector.selectNow();
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
    int acceptedCount = 0;

    private void processKeys(Iterator<SelectionKey> it){
        while (it.hasNext()){
                SelectionKey selectionKey = it.next();
                it.remove();

                SocketChannel socketChannel = null;



                do {

                    try {

                        socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();

                        if (socketChannel == null) {
                            LOG.info("socketChannel null así que retorno");
                            break;
                        }

                        // increase the accepted count
                        acceptedCount++;
                        // here i can configure the socketChannel

                        // Socket timeout, 1 min of inactivity
                        socketChannel.socket().setSoTimeout(sessionConfig.getSocketTimeout());

                        // Socket max read buffer
                        socketChannel.socket().setReceiveBufferSize(sessionConfig.getSocketReadBufferSize());

                        // creo la session con configuracion por default y la cola de escritos por default
                        BaseSession s = BaseSession.clientSessionFactory(serverPortType, socketChannel, ioHandler, sessionConfig, new DefaultWriteRequestQueu());

                        // seteo el procesador para la session
                        s.setProcessor(ioService.getIoProcessor());

                        // inicializo la session creada
                        initSession(s);

                        // add session to the waiting new session queue
                        ioService.getIoProcessor().add(s);
                    }catch (Exception e){
                        LOG.error("Exception Main acceptor loop of: "+serverPortType,e);
                    }

//                try {
//                    // fire session create event
//                    s.getIoHandler().sessionCreated(s);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

                }while (socketChannel!=null);
                LOG.info("***Round accepted count: "+acceptedCount);
        }

    }

    private void initSession(BaseSession s) {
        // init session with parameters like time, etc..

        // set Attribute map
        s.setAttributeMap(new DefaultIoSessionAttributeMap());

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
                        Thread.sleep(50);
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
