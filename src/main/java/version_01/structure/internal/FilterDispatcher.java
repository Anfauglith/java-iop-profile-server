package version_01.structure.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.filter.executor.ExecutorFilter;
import version_01.core.filter.protocol.InvalidProtocolViolation;
import version_01.core.listener.SupportIoListener;
import version_01.core.service.IoProcessor;
import version_01.core.session.IdleStatus;
import version_01.core.session.IoSession;
import version_01.core.write.SessionCloseException;
import version_01.core.write.WriteRequest;
import version_01.ssl.SslFilterManager;
import version_01.core.filter.protocol.ProtocolDecoderFilter;
import version_01.core.filter.protocol.ProtocolEncoderFilter;
import version_01.structure.filters.executor.ExecutorFilterManager;
import version_01.structure.messages.MessageFactory;


import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by mati on 11/09/16.
 *
 * Clase encargada de manejar el filtro de mensajes entrantes y salientes
 */
public class FilterDispatcher implements SupportIoListener{

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(FilterDispatcher.class);
    /** Protocol decoder filter */
    private ProtocolDecoderFilter protocolDecoderFilter;
    /** Protocol encoder filter */
    private ProtocolEncoderFilter protocolEncoderFilter;
    /** TLS/SSL filter manager */
    private SslFilterManager sslFilterManager = new SslFilterManager();
    /** Workers */
    private ExecutorFilter executorFilter = new ExecutorFilterManager(this);

    public FilterDispatcher() {
    }

    @Override
    public void fireSessionCreated(IoSession session) {
        try {
            session.getIoHandler().sessionCreated(session);
        } catch (Exception e) {
            launchEventException(session,"SessionCreated exception",e);
        }
    }

    @Override
    public void fireMessageSent(IoSession session, WriteRequest writeRequest){
        LOG.info("FireMessageSent - executorFilter");
        executorFilter.messageSent(session,writeRequest);
    }

    @Override
    public void fireInputClosed(IoSession session) {
        try{
            session.getIoHandler().inputClosed(session);
        }catch (Exception e){
            launchEventException(session,"fireInputClosed exception",e);
        }
    }

    @Override
    public void fireSessionDestroyed(IoSession session) {
        try{
            if (session.isSecure())sslFilterManager.close(session);
            session.getIoHandler().sessionClosed(session);
        }catch (Exception e){
            launchEventException(session,"fireSessionDestroyed exception",e);
        }
    }

    @Override
    public void fireExceptionCaught(IoSession session,String message, Exception e) {
        launchEventException(session,message,e);
    }

    // Metodo solo para testeo, esto no deberia ir acá
    @Override
    public int filterRead(IoSession session, ByteBuffer buf) throws IOException {
        return sslFilterManager.filterRead(session,buf);
    }

    @Override
    public int filterWrite(IoSession session, ByteBuffer buf) throws IOException {
        return sslFilterManager.filterWrite(session,buf);
    }

    @Override
    public void fireSessionIdle(IoSession session,IdleStatus status) {
        session.increaseIdleCount(status, System.currentTimeMillis());
        LOG.info("Idle session.."+status);
    }

    /**
     * Metodo llamado al recibir data por el canal (Esto quiere decir que el OP_READ se activó y se leyó data del canal agregandola al byteBuffer que llega como parametro)
     * La data pasa por todos los filtros para luego ser enviada al handler de la sesion.
     *
     * todo: seguramente acá debiera hacer el filtro del ssl al principio
     *
     * @param session
     * @param buf
     */
    @Override
    public void fireMessageReceived(IoSession session, ByteBuffer buf) {
        LOG.info("FireMessageReceived - executorFilter");
        executorFilter.messageReceived(session,buf);
    }



    /**
     *  Metodo ejecutado en thread worker llamado por el executor filter.
     *
     * @param session
     * @param buf
     */
    public void executeMessageReceived(IoSession session, ByteBuffer buf){
        Object o = buf;
        if (protocolDecoderFilter!=null) {
            try {
                o = protocolDecoderFilter.decode(buf);
            } catch (InvalidProtocolViolation e){
                try {
                    session.write(MessageFactory.buildInvalidMessageHeaderResponse());
                    // close session , todo: acá deberia ver si llega a enviar el mensaje antes de cerrar la sesión.
                    session.close();
                } catch (SessionCloseException e1) {
                    // nothing to do the session is already closed..
                }
            } catch (Exception e) {
                launchEventException(session, "ProtocolDecoder exception", e);
            }
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

    public void executeMessageSent(IoSession session,WriteRequest writeRequest){
        try {
            session.getIoHandler().messageSent(session,writeRequest.getMessage());
        } catch (Exception e) {
            launchEventException(session,"MessageSent exception.",e);
        }
    }


    /**
     * Metodo llamado al hacer un send desde la sesion.
     * Debe pasar por todos los filtros para luego ser agregada la data filtrada a la cola de writeRequest de la sesion y
     * agrega la sesion a la lista de sesiones a flushear. Haciendo así que en el proximo loop del IoProcessor se procese la sesion
     * con todos sus WriteRequest.
     * todo: seguramente acá deberia hacer el filtro de ssl al principio
     *
     * @param ioProcessor
     * @param session
     * @param writeRequest
     * @param <S>
     */
    @Override
    public <S extends IoSession> void fireSendMessage(IoProcessor<S> ioProcessor, S session, WriteRequest writeRequest) {
        try {
            ByteBuffer byteBuffer = protocolEncoderFilter.encode(writeRequest.getMessage());
            writeRequest.setFilteredMessage(byteBuffer);
            session.addWriteRequest(writeRequest);
            ioProcessor.flush(session);
        }catch (NullPointerException e){
            if (protocolEncoderFilter==null){
                throw new RuntimeException("ProtocolEnconderFilter is null, please add a protocol to the server");
            }else{
                e.printStackTrace();
            }
        }

    }

    private void launchEventException(IoSession session, String message, Throwable e){
        try {
            session.getIoHandler().exceptionCaught(session,new Exception(message,e));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void setProtocolDecoderFilter(ProtocolDecoderFilter protocolDecoderFilter) {
        this.protocolDecoderFilter = protocolDecoderFilter;
    }

    public void setProtocolEncoderFilter(ProtocolEncoderFilter protocolEncoderFilter) {
        this.protocolEncoderFilter = protocolEncoderFilter;
    }

    public ProtocolDecoderFilter getProtocolDecoderFilter() {
        return protocolDecoderFilter;
    }

    public ProtocolEncoderFilter getProtocolEncoderFilter() {
        return protocolEncoderFilter;
    }


}
