package version_01.structure.handlers;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.service.IoHandler;
import version_01.core.session.IoSession;
import version_01.structure.messages.MessageFactory;
import version_01.structure.protos.TestProto3;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mati on 24/09/16.
 */
public class CPHandler implements IoHandler{

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(CPHandler.class);

    static AtomicInteger openSessions = new AtomicInteger(0);
    static AtomicInteger closedSessions = new AtomicInteger(0);

    public static String PING_RESPONSE_PAYLOAD = "pong";
    public static String VERSION = "1";

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        LOG.debug("Puerto customer, Creé una nueva sesion!!");
        LOG.info("Puerto customer, Cantidad de sesiones abiertas: "+ openSessions.incrementAndGet());
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {

    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        LOG.debug("Cerré una sesion!!");
        openSessions.decrementAndGet();
        LOG.info("Cantidad de sesiones cerradas: "+ closedSessions.incrementAndGet());
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        LOG.info("Puerto customer, Mensaje llegó: " + message);
        LOG.info("Puerto customer, Ahora voy a enviar uno..");
        if (message instanceof TestProto3.Message) {
            TestProto3.Message messageToResponse = MessageFactory.buildPingResponseMessage(TestProto3.PingResponse.newBuilder().setPayload(ByteString.copyFromUtf8(PING_RESPONSE_PAYLOAD)).build(),VERSION);
            session.write(messageToResponse);
        }

    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        LOG.info("Puerto customer, Mandé un mensaje: " + message);
    }

    @Override
    public void inputClosed(IoSession session) throws Exception {
        LOG.info("Puerto customer, InputClosed: "+session);
    }
}
