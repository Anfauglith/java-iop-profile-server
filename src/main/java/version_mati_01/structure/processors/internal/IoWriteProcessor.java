package version_mati_01.structure.processors.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_mati_01.core.polling.AbstractProcessor;
import version_mati_01.core.write.WriteRequest;
import version_mati_01.core.write.WriteRequestQueue;
import version_mati_01.structure.processors.interfaces.WriteManager;
import version_mati_01.structure.session.BaseSession;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by mati on 17/09/16.
 */
public class IoWriteProcessor implements WriteManager<BaseSession> {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(IoWriteProcessor.class);

    private AbstractProcessor<BaseSession> processor;

    public IoWriteProcessor() {
    }

    public boolean flushNow(BaseSession session, long currentTime) {
        if (!session.isConnected()) {
            LOG.debug("FlushNow session not connected");
            processor.remove(session);
            return false;
        }

        final boolean hasFragmentation = false;//session.getTransportMetadata().hasFragmentation();

        final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();

        // Set limitation for the number of written bytes for read-write
        // fairness. I used maxReadBufferSize * 3 / 2, which yields best
        // performance in my experience while not breaking fairness much.
        final int maxWrittenBytes = session.getConfig().getMaxReadBufferSize()
                + (session.getConfig().getMaxReadBufferSize() >>> 1);

        int writtenBytes = 0;
        WriteRequest writeRequest = null;

        try {
            // Clear OP_WRITE ,
            processor.setInterestedInWrite(session, false);

            do {
                // Check for pending writes.
                writeRequest = session.getCurrentWriteRequest();

                if (writeRequest == null) {
                    writeRequest = writeRequestQueue.poll();

                    if (writeRequest == null) {
                        break;
                    }

                    session.setCurrentWriteRequest(writeRequest);
                }

                int localWrittenBytes = 0;
                Object message = writeRequest.getMessageFiltered();

                if (message instanceof ByteBuffer) {
                    localWrittenBytes = writeBuffer(session, writeRequest, hasFragmentation, maxWrittenBytes - writtenBytes,
                            currentTime);

                    if ((localWrittenBytes > 0) && ((ByteBuffer) message).hasRemaining()) {
                        // the buffer isn't empty, we re-interest it in writing
                        writtenBytes += localWrittenBytes;
                        processor.setInterestedInWrite(session, true);
                        return false;
                    }

                    // si se escribió ya puedo dar de alta el send
                    if (((ByteBuffer) message).limit() == ((ByteBuffer) message).position()){
//                        writeRequest.getFuture().notify(); ver si hacer algo así
                        session.setCurrentWriteRequest(null);
                    }
                } else {
                    throw new IllegalStateException("Don't know how to handle message of type '"
                            + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
                }

                if (localWrittenBytes == 0) {
                    // Kernel buffer is full.
                    processor.setInterestedInWrite(session, true);
                    return false;
                }

                writtenBytes += localWrittenBytes;

                if (writtenBytes >= maxWrittenBytes) {
                    // Wrote too much, lo vuelvo a la cola para que vuelva a ser flusheada
                    processor.scheduleFlush(session);
                    return false;
                }


            } while (writtenBytes < maxWrittenBytes);
        } catch (Exception e) {
            if (writeRequest != null) {
                if (writeRequest.getFuture()!=null)
                    writeRequest.getFuture().setException(e);
            }

            processor.getSupportIoListener().fireExceptionCaught(session,"Exception trying flush session",e);
            return false;
        }

        return true;
    }


    private int writeBuffer(BaseSession session, WriteRequest writeRequest, boolean hasFragmentation, int maxLength, long currentTime)
            throws Exception {
        ByteBuffer buf = (ByteBuffer) writeRequest.getMessageFiltered();
        int localWrittenBytes = 0;

        if (buf.hasRemaining()) {
            int length;

            if (hasFragmentation) {
                length = Math.min(buf.remaining(), maxLength);
            } else {
                length = buf.remaining();
            }

            try {
                localWrittenBytes = write(session, buf, length);
            } catch (IOException ioe) {
                //todo: no me concence para nada esto así..
                // We have had an issue while trying to send data to the
                // peer : let's close the session.
                buf = null;
                session.closeNow();
                processor.destroy(session);

                return 0;
            }

        }

//        session.increaseWrittenBytes(localWrittenBytes, currentTime);

        if (!buf.hasRemaining() || (!hasFragmentation && (localWrittenBytes != 0))) {
            // Buffer has been sent, clear the current request.
            int pos = buf.position();
            if (buf.mark().position()>-1)
                buf.reset();

            processor.getSupportIoListener().fireMessageSent(session, writeRequest);

            // And set it back to its position
            buf.position(pos);
        }

        return localWrittenBytes;
    }

    /**
     * Write to the channel
     *
     * todo: acá deberia
     *
     * @param session
     * @param buf
     * @param length
     * @return
     * @throws Exception
     */
    protected int write(BaseSession session, ByteBuffer buf, int length) throws Exception {

        if (buf.remaining() <= length) {
            // se que no lo tengo que hacer acá pero bueno, es para probar..
            if (session.isSecure()){
                return processor.getSupportIoListener().filterWrite(session,buf);
            }else {
                return session.getChannel().write(buf);
            }
        }

        int oldLimit = buf.limit();
        buf.limit(buf.position() + length);
        try {
            return session.getChannel().write(buf);
        } finally {
            buf.limit(oldLimit);
        }
    }

    public void setProcessor(AbstractProcessor processor) {
        if (this.processor!=null) throw new IllegalArgumentException("Processor is already setted");
        this.processor = processor;
    }
}
