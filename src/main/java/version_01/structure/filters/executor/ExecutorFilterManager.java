package version_01.structure.filters.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.filter.executor.ExecutorFilter;
import version_01.core.session.IoSession;
import version_01.core.write.WriteRequest;
import version_01.structure.internal.FilterDispatcher;

import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mati on 21/09/16.
 */
public class ExecutorFilterManager implements ExecutorFilter{

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(ExecutorFilterManager.class);
    /** EventManagerFilter */
    private final FilterDispatcher filterManager;
    /** The associated executor */
    private ExecutorService executor;

    /** The default pool size */
    private static final int DEFAULT_MAX_POOL_SIZE = 16;

    /** The number of thread to create at startup */
    private static final int BASE_THREAD_NUMBER = 0;

    /** The default KeepAlive time, in seconds */
    private static final long DEFAULT_KEEPALIVE_TIME = 30;


    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link }, no thread in the pool, and a
     * maximum of 16 threads in the pool. All the event will be handled
     * by this default executor.
     */
    public ExecutorFilterManager(FilterDispatcher filterManager) {
        this.filterManager = filterManager;
        // Create a new default Executor
        ExecutorService executor = createDefaultExecutor(BASE_THREAD_NUMBER, DEFAULT_MAX_POOL_SIZE, DEFAULT_KEEPALIVE_TIME,
                TimeUnit.SECONDS, defaultThreadFactory(), new SynchronousQueue());

        // Initialize the filter
        init(executor);
    }

    /**
     * Creates a new instance of ExecutorFilter. This private constructor is called by all
     * the public constructor.
     *
     * @param executor The underlying {@link Executor} in charge of managing the Thread pool.
     */
    private void init(ExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        this.executor = executor;
    }

    /**
     * Create an OrderedThreadPool executor.
     *
     * @param corePoolSize The initial pool sizePoolSize
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     * @param threadFactory The factory used to create threads
     * @param blockingQueue The queue used to store events
     * @return An instance of the created Executor
     */
    private ExecutorService createDefaultExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                           ThreadFactory threadFactory, BlockingQueue blockingQueue) {
        // Create a new Executor
        ExecutorService executor = new ThreadPoolExecutor(corePoolSize,maximumPoolSize,keepAliveTime,unit,blockingQueue,threadFactory);
        return executor;
    }


    /**
     * ThreadFactory
     * @return
     */
    private ThreadFactory defaultThreadFactory(){
        return new ThreadFactory() {

            private AtomicInteger threadIdGenerator = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Worker-thread-"+threadIdGenerator.incrementAndGet());
                return thread;
            }
        };
    }

    /**
     * Shuts down the underlying executor if this filter hase been created via
     * a convenience constructor.
     */
    @Override
    public void destroy() {
            executor.shutdown();
    }

    /**
     * @return the underlying {@link Executor} instance this filter uses.
     */
    public final Executor getExecutor() {
        return executor;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final void messageReceived(IoSession session, ByteBuffer message) {
        execute(()->{filterManager.executeMessageReceived(session,message);});
    }

    @Override
    public void messageSent(IoSession session, WriteRequest writeRequest) {
        execute(()->{filterManager.executeMessageSent(session,writeRequest);});
    }

    private void execute(Runnable runnable){
        executor.submit(runnable);
    }

}
