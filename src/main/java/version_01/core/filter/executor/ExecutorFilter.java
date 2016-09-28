package version_01.core.filter.executor;

import version_01.core.session.IoSession;
import version_01.core.write.WriteRequest;

import java.nio.ByteBuffer;

/**
 * Created by mati on 21/09/16.
 */
public interface ExecutorFilter {

    void messageReceived(IoSession session, ByteBuffer message);

    void messageSent(IoSession session, WriteRequest writeRequest);

    void destroy();
}
