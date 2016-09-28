package version_01.structure.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.service.IoHandler;
import version_01.core.session.IoSession;
import version_01.structure.protos.TestProto3;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mati on 24/09/16.
 */
public class NCPHandler implements IoHandler {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(NCPHandler.class);

    public static String VERSION = "1";

    static AtomicInteger openSessions = new AtomicInteger(0);
    static AtomicInteger closedSessions = new AtomicInteger(0);

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        LOG.debug("NON-CUSTOMER, Creé una nueva sesion!!");
        LOG.info("NON-CUSTOMER, Cantidad de sesiones en puerto primario abiertas: "+ openSessions.incrementAndGet());
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {

    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        LOG.debug("NON-CUSTOMER, Cerré una sesion!!");
        openSessions.decrementAndGet();
        LOG.info("Cantidad de sesiones en puerto NON-CUSTOMER cerradas: "+ closedSessions.incrementAndGet());
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        LOG.info("Puerto NON-CUSTOMER, mensaje llegó: " + message);
        LOG.info("Ahora voy a responder..");
        if (message instanceof TestProto3.Message) {
            //acá va el homeNodeRequestRequest..
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        LOG.info("Puerto NON-CUSTOMER, Mandé un mensaje: " + message);
    }

    @Override
    public void inputClosed(IoSession session) throws Exception {
        LOG.info("Puerto NON-CUSTOMER, InputClosed: "+session);
    }
}
