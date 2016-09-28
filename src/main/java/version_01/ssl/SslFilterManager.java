package version_01.ssl;

import version_01.core.session.IoSession;
import version_01.util.AttributeKey;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by mati on 19/09/16.
 */
public class SslFilterManager {

    static final AttributeKey SSL_FILTER = new AttributeKey(SslFilter.class, "filter");

    public int filterRead(IoSession session, ByteBuffer buf) throws IOException {
        return getSSLFilter(session).filterRead(session,buf);
    }

    public int filterWrite(IoSession session, ByteBuffer buf) throws IOException {
        return getSSLFilter(session).filterWrite(session,buf);
    }

    public void close(IoSession session) throws IOException {
        getSSLFilter(session).close(session);
    }

    private SslFilter getSSLFilter(IoSession session){
        return (SslFilter) session.getAttribute(SSL_FILTER);
    }


}
