package version_mati_01;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_mati_01.core.listener.SupportIoListener;
import version_mati_01.core.polling.NioNodeIoProcessor;
import version_mati_01.core.service.IoProcessor;
import version_mati_01.core.service.IoService;
import version_mati_01.core.session.IoSession;
import version_mati_01.structure.ClientSession;
import version_mati_01.core.write.WriteRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mati on 10/09/16.
 */
public class NioIoProcessorManager implements IoProcessor<ClientSession>{

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(NioIoProcessorManager.class);
    /** Processors pool */
    private Map<Integer,NioNodeIoProcessor> poolProcessors;
    /** Sessions maped between processors (sessionId-ProcessorId) */
    private Map<Long,Integer> sessionBetweenProcessors;
    /** Base ioService */
    private IoService ioService;
    /** The executor to use when we need to start the inner Processor */
    private Executor executor;
    /** Executor default thread count */
    private int processorCount = Runtime.getRuntime().availableProcessors()+1;
    /** Last processor which get a new session. Used to mantain equity between processors */
    private int lastProcessorNewSessionAdded = 0;
    /** Support listener to fire session events */
    private SupportIoListener supportIoListener;

    public NioIoProcessorManager(IoService ioService) {
        this.ioService = ioService;
        poolProcessors = new HashMap<>();
        sessionBetweenProcessors = new HashMap<>();
        init();
    }

    public NioIoProcessorManager(IoService ioService, int processorCount) {
        this.ioService = ioService;
        poolProcessors = new HashMap<>();
        sessionBetweenProcessors = new HashMap<>();
        this.processorCount = processorCount;
        init();
    }

    private void init(){
        for (int i = 0; i< processorCount; i++){
            NioNodeIoProcessor nioNodeIoProcessor = new NioNodeIoProcessor(ioService,i);
            poolProcessors.put(i,nioNodeIoProcessor);
        }
    }

    public void start(){
        ThreadFactory threadFactory = new ThreadFactory() {
            private AtomicInteger threadNumbers = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("IoProcessor-Thread-"+threadNumbers.incrementAndGet());
                return thread;
            }
        };
        executor = Executors.newFixedThreadPool(processorCount,threadFactory);
        poolProcessors.values().forEach(p->{
            p.setSupportIoListener(supportIoListener);
            executor.execute(p.startupProcessorRunnable());
        });
    }

    public void setProcessorCount(int processorCount) {
        this.processorCount = processorCount;
    }

    @Override
    public boolean isDisposing() {
        return false;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void dispose() throws Exception {

    }

    /**
     * Metodo que equilibra la carga de nuevas sesiones entres los procesadores activos
     *
     * @param session The added session
     */
    @Override
    public synchronized void add(ClientSession session) {
        // Add session to a processor
        poolProcessors.get(lastProcessorNewSessionAdded).add(session);
        // Map session with processor
        sessionBetweenProcessors.put(session.getId(),lastProcessorNewSessionAdded);
        // Next processor
        lastProcessorNewSessionAdded = (lastProcessorNewSessionAdded == poolProcessors.size() - 1) ? 0 : lastProcessorNewSessionAdded + 1;
    }

    @Override
    public void flush(ClientSession session) {

    }

    /**
     *  Method to divide the session work between processors
     *
     * @param session The session we want the message to be written
     * @param writeRequest the WriteRequest to write
     */
    @Override
    public void write(ClientSession session, WriteRequest writeRequest) {
        poolProcessors.get(sessionBetweenProcessors.get(session.getId())).write(session,writeRequest);
    }


    @Override
    public void updateTrafficControl(ClientSession session) {

    }

    @Override
    public void remove(ClientSession session) {

    }

    public void setSupportIoListener(SupportIoListener supportIoListener) {
        this.supportIoListener = supportIoListener;
    }
}
