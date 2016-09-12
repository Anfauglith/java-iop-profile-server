package version_mati_01.structure;

import version_mati_01.core.listener.SupportIoListener;
import version_mati_01.core.polling.NioNodeIoProcessor;
import version_mati_01.core.session.IoSession;
import version_mati_01.core.write.WriteRequest;
import version_mati_01.structure.filters.ProtocolDecoderFilter;
import version_mati_01.structure.filters.ProtocolEncoderFilter;

import java.nio.ByteBuffer;

/**
 * Created by mati on 11/09/16.
 */
public class DefaultSupportIoListener implements SupportIoListener {


    private ProtocolDecoderFilter protocolDecoderFilter;

    private ProtocolEncoderFilter protocolEncoderFilter;

    public DefaultSupportIoListener() {
    }

    @Override
    public void fireSessionCreated(ClientSession session) {
        try {
            session.getIoHandler().sessionCreated(session);
        } catch (Exception e) {
            launchEventException(session,"SessionCreated exception",e);
        }
    }

    @Override
    public void fireMessageReceived(ClientSession session, ByteBuffer buf) {
        Object o = null;
        try {
            o = protocolDecoderFilter.decode(buf);
        }catch (Exception e){
            launchEventException(session,"ProtocolDecoder exception",e);
        }
        // chequeo si el objeto fue filtrado correctamente
        if (o!=null) {
            try {
                session.getIoHandler().messageReceived(session, o);
            } catch (Exception e) {
                launchEventException(session,"MessageReceived exception.",e);
            }
        }
    }

    @Override
    public void fireMessageSent(ClientSession session, WriteRequest writeRequest){
        try {
            session.getIoHandler().messageSent(session,writeRequest.getMessage());
        } catch (Exception e) {
            launchEventException(session,"MessageSent exception.",e);
        }
    }

    @Override
    public void fireExceptionCaught(ClientSession session,String message, Exception e) {
        launchEventException(session,message,e);
    }

    @Override
    public <S extends ClientSession> void fireSendMessage(NioNodeIoProcessor<S> sNioNodeIoProcessor, ClientSession session, WriteRequest writeRequest) {
        ByteBuffer byteBuffer = protocolEncoderFilter.encode(writeRequest.getMessage());
        writeRequest.setFilteredMessage(byteBuffer);
        session.addWriteRequest(writeRequest);
        sNioNodeIoProcessor.flush(session);
    }

    public void setProtocolDecoderFilter(ProtocolDecoderFilter protocolDecoderFilter) {
        this.protocolDecoderFilter = protocolDecoderFilter;
    }

    public void setProtocolEncoderFilter(ProtocolEncoderFilter protocolEncoderFilter) {
        this.protocolEncoderFilter = protocolEncoderFilter;
    }

    private void launchEventException(IoSession session, String message, Throwable e){
        try {
            session.getIoHandler().exceptionCaught(session,new Exception(message,e));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
