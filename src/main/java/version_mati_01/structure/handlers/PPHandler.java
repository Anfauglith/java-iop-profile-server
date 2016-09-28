package version_mati_01.structure.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_mati_01.core.service.IoHandler;
import version_mati_01.core.service.IoService;
import version_mati_01.core.session.IoSession;
import version_mati_01.structure.messages.MessageFactory;
import version_mati_01.structure.protos.TestProto3;
import version_mati_01.util.ConfigurationsUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mati on 24/09/16.
 */
public class PPHandler implements IoHandler {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(PPHandler.class);

    public static String VERSION = "1";

    static AtomicInteger openSessions = new AtomicInteger(0);
    static AtomicInteger closedSessions = new AtomicInteger(0);

    private IoService ioService;

    private List<TestProto3.ServerRole> rolesCached;

    public PPHandler(IoService ioService) {
        this.ioService = ioService;
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        LOG.debug("Creé una nueva sesion!!");
        LOG.info("Cantidad de sesiones en puerto primario abiertas: "+ openSessions.incrementAndGet());
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {

    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        LOG.debug("Cerré una sesion!!");
        openSessions.decrementAndGet();
        LOG.info("Cantidad de sesiones en puerto primario cerradas: "+ closedSessions.incrementAndGet());
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    /**
     *
     * @param session The session that is receiving a message
     * @param message The received message
     * @throws Exception
     *///todo: podria cachear directamente el response en vez de construirlo cada vez que lo piden.
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        LOG.info("Puerto primario, mensaje llegó: " + message);
        LOG.info("Ahora voy a responder..");
        if (message instanceof TestProto3.Message) {
            TestProto3.ListRolesRequest listRolesRequest =((TestProto3.Message) message).getRequest().getSingleRequest().getListRoles();
            if (listRolesRequest!=null){
                if (rolesCached==null) rolesCached = ConfigurationsUtil.getServerRolesConfigurations(ioService.getListPortConfigurations());
                TestProto3.Message messageToResponse = MessageFactory.buildServerListRolesResponseMessage(TestProto3.ListRolesResponse.newBuilder().addAllRole(rolesCached).build(),VERSION);
                session.write(messageToResponse);
            }
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        LOG.info("Puerto primario, Mandé un mensaje: " + message);
    }

    @Override
    public void inputClosed(IoSession session) throws Exception {
        LOG.info("Puerto primario, InputClosed: "+session);
    }
}
