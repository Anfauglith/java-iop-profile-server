package version_mati_01.core.listener;

import version_mati_01.core.polling.NioNodeIoProcessor;
import version_mati_01.core.write.WriteRequest;
import version_mati_01.structure.ClientSession;

import java.nio.ByteBuffer;

/**
 * Created by mati on 11/09/16.
 */
public interface SupportIoListener {

    // Filter session events
    void fireSessionCreated(ClientSession session);
    void fireMessageReceived(ClientSession session, ByteBuffer buf);


    <S extends ClientSession> void fireSendMessage(NioNodeIoProcessor<S> sNioNodeIoProcessor, ClientSession session, WriteRequest writeRequest);

    void fireMessageSent(ClientSession session, WriteRequest writeRequest);

    void fireExceptionCaught(ClientSession session,String message,Exception e);
}
