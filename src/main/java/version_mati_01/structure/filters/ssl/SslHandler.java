package version_mati_01.structure.filters.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_mati_01.structure.session.BaseSession;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;

/**
 * Created by mati on 12/09/16.
 */
public class SslHandler {

    /** A logger for this class */
    private final static Logger LOGGER = LoggerFactory.getLogger(SslHandler.class);

    /** The SSL Filter which has created this handler */
    private final SslFilter sslFilter;

    /** The current session */
    private final BaseSession session;

    private SSLEngine sslEngine;

    /**
     * A flag set to true when the first SSL handshake has been completed
     * This is used to avoid sending a notification to the application handler
     * when we switch to a SECURE or UNSECURE session.
     */
    private boolean firstSSLNegociation;

    /** A flag set to true when a SSL Handshake has been completed */
    private boolean handshakeComplete;

    private SSLEngineResult.HandshakeStatus handshakeStatus;

    /**
     * Create a new SSL Handler, and initialize it.
     *
     * @throws SSLException
     */
    SslHandler(SslFilter sslFilter, BaseSession session) throws SSLException {
        this.sslFilter = sslFilter;
        this.session = session;
    }

    /**
    * Initialize the SSL handshake.
    *
    * @throws SSLException If the underlying SSLEngine handshake initialization failed
     */
    void init() throws SSLException {
        if (sslEngine != null) {
            // We already have a SSL engine created, no need to create a new one
            return;
        }

        LOGGER.debug("{} Initializing the SSL Handler", session);

        InetSocketAddress peer = null;//(InetSocketAddress) session.getAttribute(SslFilter.PEER_ADDRESS);

        // Create the SSL engine here
        if (peer == null) {
            sslEngine = sslFilter.sslContext.createSSLEngine();
        } else {
            sslEngine = sslFilter.sslContext.createSSLEngine(peer.getHostName(), peer.getPort());
        }

        // Initialize the different SslEngine modes
        if (!sslEngine.getUseClientMode()) {
            // Those parameters are only valid when in server mode
            if (sslFilter.isWantClientAuth()) {
                sslEngine.setWantClientAuth(true);
            }

            if (sslFilter.isNeedClientAuth()) {
                sslEngine.setNeedClientAuth(true);
            }
        }

        // Set the cipher suite to use by this SslEngine instance
        if (sslFilter.getEnabledCipherSuites() != null) {
            sslEngine.setEnabledCipherSuites(sslFilter.getEnabledCipherSuites());
        }

        // Set the list of enabled protocols
        if (sslFilter.getEnabledProtocols() != null) {
            sslEngine.setEnabledProtocols(sslFilter.getEnabledProtocols());
        }

        // todo: ver que hace esto..
        sslEngine.beginHandshake();

        handshakeStatus = sslEngine.getHandshakeStatus();

        // We haven't yet started a SSL negotiation
        // set the flags accordingly
        firstSSLNegociation = true;
        handshakeComplete = false;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} SSL Handler Initialization done.", session);
        }


    }


}
