package version_01.structure.processors.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.polling.AbstractProcessor;
import version_01.core.service.IoProcessor;
import version_01.core.session.IoSessionConfig;
import version_01.structure.processors.interfaces.ReadManager;
import version_01.structure.session.BaseSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * Created by Matias Furszyferas Furszyfer on 17/09/16.
 *
 * Clase helper con los metodos de lectura y el procesamiento de la misma de una determinada session entregada por parametro.
 */
public class IoReadProcessor implements ReadManager<BaseSession> {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(IoReadProcessor.class);

    private IoProcessor<BaseSession> processor;

    public IoReadProcessor() {
    }

    public void read(BaseSession session) {

        if (session.isClosing()){
            LOG.info("read: session closing, it's not necessary to read it.");
            return;
        }

        IoSessionConfig config = session.getConfig();
        int bufferSize = config.getReadBufferSize();

        ByteBuffer buf = ByteBuffer.allocate(bufferSize);

        // por ahora lo dejo en false..
        final boolean hasFragmentation = false;//session.getTransportMetadata().hasFragmentation();

        try {
            int readBytes = 0;
            int readBuffAux;

            try {
                if (hasFragmentation) {

                    while ((readBuffAux = read(session, buf)) > 0) {
                        readBytes += readBuffAux;

                        if (!buf.hasRemaining()) {
                            break;
                        }
                    }
                } else {

                    // Check secure session, todo: esto no deberia ir acá pero tengo que probar. Despues veo como lo acomodo bien.
                    if (session.isSecure()){
                        readBuffAux = processor.getSupportIoListener().filterRead(session,buf);
                        LOG.info("readBuffAux: "+readBuffAux);
                    }else {
                        // Leo el canal
                        readBuffAux = read(session, buf);
                    }

                    if (readBuffAux > 0) {
                        readBytes = readBuffAux;
                    }
                }
            } finally {
                buf.flip();
            }

            if (readBytes > 0) {
                //launch messageReceivedEvent
                processor.getSupportIoListener().fireMessageReceived(session,buf);

                buf = null;

                if (hasFragmentation) {
                    if (readBytes << 1 < config.getReadBufferSize()) {
                        session.decreaseReadBufferSize();
                    } else if (readBytes == config.getReadBufferSize()) {
                        session.increaseReadBufferSize();
                    }
                }
            }

            if (readBuffAux < 0) {
                // si lo que se leyó es menor que 0 quiere decir que el inputStream tiró un EOF y por ende está cerrado.
                // Así que preparo para cerrar la sesion y lanzo el evento de que se cerró el input
                processor.remove(session);
                processor.getSupportIoListener().fireInputClosed(session);
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                LOG.error("IOException, removing session, "+session.getId(),e);
                // se debe eliminar la sesion
                session.getProcessor().remove(session);

            }else {
                try {
                    processor.getSupportIoListener().fireExceptionCaught(session, "Exception reading from session..", e);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * Leo el canal directamente.
     * todo: acá deberia ver si es un canal seguro y hacer la lectura del SslManager en vez del canal directamente.
     *
     * @param session
     * @param buf
     * @return
     * @throws Exception
     */
    protected int read(BaseSession session, ByteBuffer buf) throws Exception {
        ByteChannel channel = session.getChannel();
        return channel.read(buf);
    }


    public void setProcessor(AbstractProcessor processor) {
        if (this.processor!=null) throw new IllegalArgumentException("Processor is already setted");
        this.processor = processor;
    }
}
