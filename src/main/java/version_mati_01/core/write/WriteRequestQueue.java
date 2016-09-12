package version_mati_01.core.write;

import java.util.Iterator;

/**
 * Created by mati on 09/09/16.
 * Esto es una interface por las dudas que quiera implementar distintas formas de colas, quizás hasta puedo hacer una por prioridades..
 */
public interface WriteRequestQueue {

    WriteRequest poll();

    void offer(WriteRequest writeRequest);

    boolean isEmpty();

    Iterator<WriteRequest> iterator();


}