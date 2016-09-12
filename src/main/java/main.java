import version_mati_01.NioNodeServer;
import version_mati_01.core.service.IoHandler;
import version_mati_01.core.session.IoSession;

/**
 * Created by mati on 08/09/16.
 */
public class main {


    public static void main(String[] args){

        NioNodeServer nioNodeServer = new NioNodeServer();
        nioNodeServer.setIoHandler(new IoHandler() {
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                System.out.println("Creé una nueva sesion!!");
            }

            @Override
            public void sessionOpened(IoSession session) throws Exception {

            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                System.out.println("Cerré una sesion!!");
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                cause.printStackTrace();
            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                System.out.println("Mensaje llegó: " + message);
                System.out.println("Ahora voy a enviar uno..");
                String messageToReplay = "todo ok!";
                session.write(messageToReplay);
            }

            @Override
            public void messageSent(IoSession session, Object message) throws Exception {
                System.out.println("Mandé un mensaje: " + message);
            }

            @Override
            public void inputClosed(IoSession session) throws Exception {

            }
        });

        try {
            nioNodeServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
