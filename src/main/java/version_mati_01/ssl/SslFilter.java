package version_mati_01.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_mati_01.core.session.IoSession;
import version_mati_01.util.AttributeKey;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static version_mati_01.ssl.SslFilterManager.SSL_FILTER;

/**
 * Created by mati on 19/09/16.
 *
 * ver si no me conviene tener una sola instancia de esto y no irla multiplicando..
 */
public class SslFilter {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(SslFilter.class);

    private static final AttributeKey SSL_MANAGER = new AttributeKey(SslFilter.class, "manager");


    /** SSL Context */
    private SSLContext sslContext;
    /** Flag to set client/server mode */
    private boolean isClientMode;
    /** Server flags */
    // todo: seria bueno ver que hacen
    private boolean needClientAuth = false;
    private boolean wantClientAuth = false;

    /** Enabled cipher suites */
    private String[] enabledCipherSuites;
    /** Enabled protocols */
    private String[] enabledProtocols;

    public SslFilter(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * @return <tt>true</tt> if the engine is set to use isClientMode mode
     * when handshaking.
     */
    public boolean isUseClientMode() {
        return isClientMode;
    }

    /**
     * Configures the engine to use isClientMode (or server) mode when handshaking.
     *
     * @param clientMode <tt>true</tt> when we are in isClientMode mode, <tt>false</tt> when in server mode
     */
    public void setUseClientMode(boolean clientMode) {
        this.isClientMode = clientMode;
    }

    /**
     * @return <tt>true</tt> if the engine will <em>require</em> isClientMode authentication.
     * This option is only useful to engines in the server mode.
     */
    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    /**
     * Configures the engine to <em>require</em> isClientMode authentication.
     * This option is only useful for engines in the server mode.
     *
     * @param needClientAuth A flag set when we need to authenticate the isClientMode
     */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    /**
     * @return <tt>true</tt> if the engine will <em>request</em> isClientMode authentication.
     * This option is only useful to engines in the server mode.
     */
    public boolean isWantClientAuth() {
        return wantClientAuth;
    }

    /**
     * Configures the engine to <em>request</em> isClientMode authentication.
     * This option is only useful for engines in the server mode.
     *
     * @param wantClientAuth A flag set when we want to check the isClientMode authentication
     */
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }

    /**
     * @return the list of cipher suites to be enabled when {@link SSLEngine}
     * is initialized. <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

    /**
     * Sets the list of cipher suites to be enabled when {@link SSLEngine}
     * is initialized.
     *
     * @param cipherSuites <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public void setEnabledCipherSuites(String[] cipherSuites) {
        this.enabledCipherSuites = cipherSuites;
    }

    /**
     * @return the list of protocols to be enabled when {@link SSLEngine}
     * is initialized. <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }

    /**
     * Sets the list of protocols to be enabled when {@link SSLEngine}
     * is initialized.
     *
     * @param protocols <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public void setEnabledProtocols(String[] protocols) {
        this.enabledProtocols = protocols;
    }

    /**
     * Filter initializer method.
     * Esto sucede en el momento que la sesion es removida de la cola de waiting e iniciada por los processors.
     * @param session
     */
    public void onPreAdd(IoSession session){
        // Check that we don't have a SSL filter already present in the session
        // todo: no creo que esto llegue a saltar nunca..
        if (session.containsAttribute(SSL_FILTER) || session.containsAttribute(SSL_MANAGER)){
            String msg = "Only one SSL filter is permitted in a sesion.";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        LOG.debug("Adding the SSL Filter {} to the session: "+session.getId());

        // Create the SSL Manager
        InetSocketAddress socketAddress = session.getLocalAddress();
        SSLEngine sslEngine = sslContext.createSSLEngine(socketAddress.getHostName(),socketAddress.getPort());
        sslEngine.setUseClientMode(isClientMode);
        sslEngine.setNeedClientAuth(needClientAuth);
        sslEngine.setWantClientAuth(wantClientAuth);
        SslManager sslManager = new SslManager((SocketChannel) session.getChannel(),sslEngine);
        // Secure flag
        session.setSecure(true);
        // Add sslFilter to the session
        session.setAttribute(SSL_FILTER,this);
        // Add sslManager to the session
        session.setAttribute(SSL_MANAGER, sslManager);

    }


    public int filterRead(IoSession session,ByteBuffer buf) throws IOException {
        SslManager sslManager = (SslManager) session.getAttribute(SSL_MANAGER);
        ByteBuffer request = sslManager.getAppInpBuffer();
        int count = sslManager.read();
        LOG.info("session id: "+session.getId()+" read count: "+count+" request: "+request);
        if (count<0){
            //todo: esto no creo que tenga que ir así, deberia lanzar un evento para que el processor la termine de eliminar
            // client has closed
            try {
                sslManager.close();
            }catch (IOException e){
                // nothing
            }
            session.closeNow();
            // finished with this key
            session.getSelectionKey().cancel();
            // finished with this socket actually
            session.getChannel().close();
        } else
            if (request.position() > 0){
                // client request, data readed
                LOG.debug("reading received data: "+ new String(request.array(),0,request.position()));
//                buf.put(request.array(),0,request.position());

                //data readed
                request.flip();
                buf.put(request);
                request.compact();
            }

        LOG.info("handshake status: "+ sslManager.getEngine().getHandshakeStatus());

        return count;

    }


    public int filterWrite(IoSession session, ByteBuffer buf) throws IOException {
        SslManager sslManager = (SslManager) session.getAttribute(SSL_MANAGER);
        ByteBuffer reply = sslManager.getAppOutBuffer();
        // put data to send in to the buffer..
        reply.put(buf);

        LOG.info("writing "+reply);
        int count = 0;
        while (reply.position() > 0){
//            reply.flip();
            count = sslManager.write();
//            reply.compact();
            if (count == 0){
                break;
            }
        }
        if (reply.position() > 0){
            // short write
            // Register for OP_WRITE and come bacj here when ready
            session.getSelectionKey().interestOps(session.getSelectionKey().interestOps() | SelectionKey.OP_WRITE);
        } else {
            // Write succeded, don't need OP_WRITE any more
            session.getSelectionKey().interestOps(session.getSelectionKey().interestOps() &~ SelectionKey.OP_WRITE);
        }
        return count;
    }

    public void close(IoSession session) throws IOException {
        try {
            SslManager sslManager = (SslManager) session.getAttribute(SSL_MANAGER);
            sslManager.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
