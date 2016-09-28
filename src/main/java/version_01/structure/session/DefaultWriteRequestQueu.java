package version_01.structure.session;

import version_01.core.write.WriteRequest;
import version_01.core.write.WriteRequestQueue;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by mati on 11/09/16.
 */
public class DefaultWriteRequestQueu implements WriteRequestQueue{


    private Queue<WriteRequest> queue;

    public DefaultWriteRequestQueu() {
        queue = new ConcurrentLinkedDeque<>();
    }

    @Override
    public WriteRequest poll() {
        return queue.poll();
    }

    @Override
    public void offer(WriteRequest writeRequest) {
        queue.offer(writeRequest);
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public Iterator<WriteRequest> iterator() {
        return queue.iterator();
    }

    @Override
    public String toString() {
        return "DefaultWriteRequestQueu{" +
                "queue=" + queue +
                '}';
    }
}
