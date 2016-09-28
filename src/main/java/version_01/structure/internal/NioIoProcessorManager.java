package version_01.structure.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.configuration.ServerPortType;
import version_01.core.listener.SupportIoListener;
import version_01.structure.processors.CIoProcessor;
import version_01.core.service.IoProcessor;
import version_01.core.service.IoService;
import version_01.structure.processors.PPortProcessor;
import version_01.structure.session.BaseSession;
import version_01.core.write.WriteRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mati on 10/09/16.
 */
public class NioIoProcessorManager implements IoProcessor<BaseSession>{

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(NioIoProcessorManager.class);
    /** Processors pool */
    private Map<ServerPortType,Map<Integer,IoProcessor>> poolProcessors;
    /** Sessions maped between processors (sessionId-ProcessorId), esto es el mapeo de que session está siendo atendida por que processor */
    private Map<Long,Integer> sessionBetweenProcessors;
    /** Base ioService */
    private IoService ioService;
    /** The executor to use when we need to start the inner Processor */
    private Map<ServerPortType,Executor> executors;
    /** Executor default thread count */
//    private int processorCount = Runtime.getRuntime().availableProcessors()+1;
    /** Last processor which get a new session. Used to mantain equity between processors */
//    private int lastProcessorNewSessionAdded = 0;
    private Map<ServerPortType,Integer> lastProcessorSessionAddedByPortMap;
    /** Support listener to fire session events */
    private Map<ServerPortType,SupportIoListener> supportIoListener;


    public NioIoProcessorManager(IoService ioService) {
        this.ioService = ioService;
        poolProcessors = new HashMap<>();
        sessionBetweenProcessors = new HashMap<>();
        executors = new HashMap<>();
        lastProcessorSessionAddedByPortMap = new HashMap<>();
        supportIoListener = new HashMap<>();
    }


    public void start(){
        try {
            poolProcessors.forEach((serverPortType, processorMap) -> {
                ThreadFactory threadFactory = new ThreadFactory() {
                    private AtomicInteger threadNumbers = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("IoProcessor-" + serverPortType + "-Thread-" + threadNumbers.incrementAndGet());
                        return thread;
                    }
                };
                ExecutorService executor = Executors.newFixedThreadPool(processorMap.size(), threadFactory);
                executors.put(serverPortType, executor);
                processorMap.values().forEach(p -> {
                    p.setSupportIoListener(supportIoListener.get(serverPortType));
                    executor.execute(p.startupProcessorRunnable());
                });
                /** Log to inform situation */
                LOG.info("PoolProcessors on port " + serverPortType + " started \nProcessors count: " + processorMap.size());
            });

            /** Log to inform situation */
            LOG.info("ProcessorManager started \nProcessors count: " + poolProcessors.size());
        }catch (Exception e){
            e.printStackTrace();
        }
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
        // nothing
        throw new RuntimeException("Method not available");
    }

    /**
     * Metodo que equilibra la carga de nuevas sesiones entres los procesadores activos
     *
     * @param session The added session
     */
    @Override
    public synchronized void add(BaseSession session) {
        int lastProcessorNewSessionAdded = lastProcessorSessionAddedByPortMap.get(session.getServerPortType());
        LOG.info("add new session for: "+session.getServerPortType()+" go to processor number: "+lastProcessorNewSessionAdded+", cantidad de session añadidas (sin contar esta): "+sessionBetweenProcessors.size());
        // Add session to a processor
        poolProcessors.get(session.getServerPortType()).get(lastProcessorNewSessionAdded).add(session);
        // Map session with processor
        sessionBetweenProcessors.put(session.getId(),lastProcessorNewSessionAdded);
        // Next processor
        lastProcessorNewSessionAdded = (lastProcessorNewSessionAdded == poolProcessors.size() - 1) ? 0 : lastProcessorNewSessionAdded + 1;
    }

    @Override
    public void flush(BaseSession session) {
        // nothing
        throw new RuntimeException("Method not available");
    }

    /**
     *  Method to divide the session work between processors
     *
     * @param session The session we want the message to be written
     * @param writeRequest the WriteRequest to write
     */
    @Override
    public void write(BaseSession session, WriteRequest writeRequest) {
        poolProcessors.get(session.getServerPortType()).get(sessionBetweenProcessors.get(session.getId())).write(session,writeRequest);
    }


    @Override
    public void updateTrafficControl(BaseSession session) {

    }

    @Override
    public void remove(BaseSession session) {
        try {
            int sessionBetweenProcessorId = sessionBetweenProcessors.get(session.getId());
            sessionBetweenProcessors.remove(session.getId());
            poolProcessors.get(session.getServerPortType()).get(sessionBetweenProcessorId).remove(session);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public Runnable startupProcessorRunnable() {
        // nothing for now
        return null;
    }

    public SupportIoListener getSupportIoListener(ServerPortType serverPortType) {
        return supportIoListener.get(serverPortType);
    }


    public void addSupportIoListener(ServerPortType portType, SupportIoListener filterManager) {
        if (supportIoListener.containsKey(portType)) throw new IllegalArgumentException("Support listener already exist for portType: "+portType);
        supportIoListener.put(portType,filterManager);
    }

    @Override
    public void scheduleFlush(BaseSession session) {
        //nothing..
        throw new RuntimeException("Method not inplemented..");
    }

    @Override
    public void setSupportIoListener(SupportIoListener supportIoListener) {
        // nothig
        throw new RuntimeException("Method not inplemented..");
    }
    @Override
    public SupportIoListener getSupportIoListener() {
        // nothig
        throw new RuntimeException("Method not inplemented..");
    }

    public void manageServerRole(ServerPortType serverPortType, int processorCount){
        Map<Integer,IoProcessor> processors = new HashMap<>();
        switch (serverPortType){
            case PRIMARY:
                initPrimaryPortProcessor(processors,processorCount);
                break;
            case CUSTOMER:
                initCustomerPortProcessor(processors,processorCount);
                break;
            case NON_CUSTOMER:
                initNonCustomerPortProcessor(processors,processorCount);
                break;
            default:
                throw new IllegalArgumentException("PortRole not managed by the actual implementation.");
        }
        poolProcessors.put(serverPortType,processors);

        // initialize
        lastProcessorSessionAddedByPortMap.put(serverPortType,0);
    }

    /**
     * Este va distinto
     * @param processorCount
     */
    private void initPrimaryPortProcessor(Map<Integer,IoProcessor> processors,int processorCount){
        for (int i = 0; i< processorCount; i++){
            IoProcessor ioProcessor = new PPortProcessor(ioService,i);
            processors.put(i,ioProcessor);
        }
    }

    /**
     *
     * @param processors
     * @param processorCount
     */
    private void initNonCustomerPortProcessor(Map<Integer, IoProcessor> processors, int processorCount){
        for (int i = 0; i< processorCount; i++){
            // usa el mismo procesador que el primario ya que es una conexiones de tipo one shot por ahora.
            IoProcessor ioProcessor = new PPortProcessor(ioService,i);
            processors.put(i,ioProcessor);
        }
    }

    /**
     *
     * @param processors
     * @param processorCount
     */
    private void initCustomerPortProcessor(Map<Integer, IoProcessor> processors, int processorCount){
        for (int i = 0; i< processorCount; i++){
            IoProcessor ioProcessor = new CIoProcessor(ioService,i);
            processors.put(i,ioProcessor);
        }
    }

    @Override
    public String toString() {
        return "NioIoProcessorManager{" +
                "poolProcessors=" + poolProcessors +
                '}';
    }


}
