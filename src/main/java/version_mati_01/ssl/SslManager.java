package version_mati_01.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by mati on 16/09/16.
 */
public class SslManager {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(SslManager.class);
    /** Channel */
    private SocketChannel socketChannel;
    /** Engine */
    private SSLEngine engine;
    /** Buffers */
    private ByteBuffer appOutBuffer;
    private ByteBuffer netOutBuffer;
    private ByteBuffer appInpBuffer;
    private ByteBuffer netInpBuffer;

    /** Result */
    private SSLEngineResult engineResult = null;

    /** A flag set to true when a SSL Handshake has been completed */
    private boolean handshakeComplete = false;

    public SslManager(SocketChannel socketChannel, SSLEngine engine) {
        this.socketChannel = socketChannel;
        this.engine = engine;
        createBuffers();
    }

    private void createBuffers(){
        SSLSession sslSession = engine.getSession();
        int netBufferSize = sslSession.getPacketBufferSize();
        int appBufferSize = sslSession.getApplicationBufferSize();

        // le agrego 50 para que no tiene un buffer overflow ya que el decode puede hacer más grande el tamañp
        this.appOutBuffer = ByteBuffer.allocate(appBufferSize+50);
        this.appInpBuffer = ByteBuffer.allocate(appBufferSize+50);

        // network encripted buffers
        this.netOutBuffer = ByteBuffer.allocate(netBufferSize);
        this.netInpBuffer = ByteBuffer.allocate(netBufferSize);
    }

    public int read() throws IOException, SSLException{

        LOG.info("SSlManager read..");

        if (engine.isInboundDone()){
            // Kind test to return another EOF
            // SocketChannels react badly
            // if you try to read at EOF more than once
            return -1;
        }

        int pos = appInpBuffer.position();

        // Read from the channel
        int count = socketChannel.read(netInpBuffer);

        // Si el canal está cerrado aviso de una.
        if (count==-1){
            engine.closeInbound();
            return count;
        }

        // Unwrap the data just read
        netInpBuffer.flip();
        engineResult = engine.unwrap(netInpBuffer,appInpBuffer);
        netInpBuffer.compact();

        switch (engineResult.getStatus()) {

            case BUFFER_UNDERFLOW:
                return 0; // nothing was read, nothing was produced
            case BUFFER_OVERFLOW:
                // no room in appInpBuffer; application must clear it
                throw new BufferOverflowException();
            case CLOSED:
                socketChannel.socket().shutdownInput(); // no more input
                // outbound close_notify will be sent by engine
                break;
            case OK:
                // method succed
                break;
        }

        //process any handshaking now required
        while(processHandshake())
            ;

        if (count == -1){
            engine.closeInbound();
            //throws SSlException if close_notift not received
        }

        if (engine.isInboundDone()){
            return -1;
        }

        // return count of application data read
        count = appInpBuffer.position() - pos;
        return count;
    }

    public int write() throws IOException, SSLException{

        LOG.debug("SSlManager write..");

        int pos = appOutBuffer.position();

        // clear network buffer
        netOutBuffer.clear();

        //wrap the data to be written
        appOutBuffer.flip();
        engineResult = engine.wrap(appOutBuffer,netOutBuffer);
        appOutBuffer.compact();

        // Process the engineResults.Status
        switch (engineResult.getStatus()){

            case BUFFER_UNDERFLOW:
                throw new BufferUnderflowException();
            case BUFFER_OVERFLOW:
                // this cannot occur if there ir a flush after every wrap, as there is here
                throw new BufferOverflowException();
            case CLOSED:
                throw new SSLException("SSLEngine is CLOSED");
            case OK:
                // operation success
                break;
        }

        //process handshakes
        while (processHandshake())
            ;

        // Flush any pending data to the network
        flush();

        // return count of application bytes written
        return pos - appOutBuffer.position();
    }

    /**
     *  Close the session and the channel
     *
     * @throws IOException
     * @throws SSLException
     */
    public void close() throws IOException, SSLException{
        try{
            // Flush any pending output data
            if (socketChannel.isOpen()) {
                flush();
            }

            if (!engine.isOutboundDone()){
                engine.closeOutbound();
                while (processHandshake())
                    ;
                /*
                * RFC 2246 #7.2.1: if we are initiating this close, we may
                * send the close_notify without waiting for an incoming close_notify.
                * If we weren't the initiator we would have already
                * received the inbound close_notify in read(),
                * and therefore already have donoe closeOutbound(),
                * so, we are initiating close,
                * so we can skip the closeInbound().
                *
                 */
            } else
                if (!engine.isInboundDone()){
                    // throws SSLException if close_notify not received.
                    engine.closeInbound();
                    processHandshake();
                }
        }finally {
            // Close the channel
            socketChannel.close();
        }
    }


    private int flush() throws IOException {
        LOG.info("Flushing data..");
        netOutBuffer.flip();
        int count = socketChannel.write(netOutBuffer);
        netOutBuffer.compact();
        LOG.info("data flushed count: "+count);
        return count;
    }

    /**
     * Process handshake status
     * @return true if handshaking can continue
     */

    private boolean processHandshake() throws IOException {

        LOG.debug("SSlManager process handshake..");

        int count;

        // process the handshake status
        switch (engine.getHandshakeStatus()){

            case NEED_TASK:
                runDelegatedTasks();
                if (engineResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP){
                    return true;
                }else {
                    if (!(netInpBuffer.position()>0))
                        return false;
                    else {
                        // tengo que continuar ya que no terminé de leer toda la data de la red..
                    }
                }
//                return true; // can't continue during tasks
            case NEED_UNWRAP:
                // Don't read if inbound is already closed
                if (!(netInpBuffer.position()<netInpBuffer.limit())) count = engine.isInboundDone() ? -1 : socketChannel.read(netInpBuffer);
                netInpBuffer.flip();
                engineResult = engine.unwrap(netInpBuffer,appInpBuffer);
                netInpBuffer.compact();
                break;
            case NEED_WRAP:
                appOutBuffer.flip();
                engineResult = engine.wrap(appOutBuffer,netOutBuffer);
                appOutBuffer.compact();

                if (engineResult.getStatus() == SSLEngineResult.Status.CLOSED){

                    /**
                     * RFC 2246 #7.2.1 requires us to respond to an incoming
                     * close_notify. The engine takes care of this, so we
                     * are now trying to send a close_notify, which can only
                     * happen if we have just received a close_notify
                     */
                    try{
                        count = flush();
                    }catch (SocketException e){
                        /**
                         *  tried but failed to send close_notify back:
                         *  this can happen if the peer has sent its
                         *  close_notify and then closed the socket
                         *  which is permitted by RFC 2246.
                         */
                        e.printStackTrace();
                    }
                }else {
                    // flush without the try catch, letting any exceptions propagate
                    count = flush();
                }
                break;
            case FINISHED:
                // Este flag quiere decir que el handshake acaba de terminar.
                handshakeComplete = true;
            case NOT_HANDSHAKING:
                // handshaking can cease.
                return false;
        }

        // Check the result of the preceding wrap or unwrap
        switch (engineResult.getStatus()){
            case BUFFER_UNDERFLOW: //fall through
            case BUFFER_OVERFLOW:
                // handshaking cannot continue.
                return false;
            case CLOSED:
                if (engine.isOutboundDone()){
                    socketChannel.socket().shutdownOutput(); //stop sending
                }
                return false;
            case OK:
                // handshaking can continue.
                break;
        }
        return true;
    }

    protected void runDelegatedTasks() {
        // run delegated tasks
        Runnable task;
        while ( (task = engine.getDelegatedTask()) != null){
            task.run();
        }
        //update engine result
        engineResult = new SSLEngineResult(
                engineResult.getStatus(),
                engine.getHandshakeStatus(),
                engineResult.bytesProduced(),
                engineResult.bytesConsumed());
    }


    public SSLEngine getEngine() {
        return engine;
    }

    public ByteBuffer getAppInpBuffer() {
        return appInpBuffer;
    }

    public ByteBuffer getAppOutBuffer() {
        return appOutBuffer;
    }
}
