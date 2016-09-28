package version_mati_01.core.listener;

import version_mati_01.core.service.IoProcessor;
import version_mati_01.core.session.IoSession;
import version_mati_01.core.write.WriteRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by mati on 11/09/16.
 */
public interface SupportIoListener {

    // Filter session events
    void fireSessionCreated(IoSession session);
    void fireMessageReceived(IoSession session, ByteBuffer buf);


    <S extends IoSession> void fireSendMessage(IoProcessor<S> processor, S session, WriteRequest writeRequest);

    void fireMessageSent(IoSession session, WriteRequest writeRequest);

    void fireInputClosed(IoSession session);

    void fireSessionDestroyed(IoSession session);

    void fireExceptionCaught(IoSession session, String message, Exception e);

    // metodo solo para testeo, despues esto no deberia ir acá
    int filterRead(IoSession session, ByteBuffer buf) throws IOException;
    // metodo solo para testeo, despues esto no deberia ir acá
    int filterWrite(IoSession session, ByteBuffer buf) throws IOException;
}
