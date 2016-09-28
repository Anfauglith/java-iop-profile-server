package version_01.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mati on 09/09/16.
 */
public class SessionUtil {


    private static final AtomicLong idGenerator = new AtomicLong(0);

    public static long sessionIdGenerator(){
        return idGenerator.incrementAndGet();
    }

}
