package version_01.structure.filters.ssl;

import version_01.structure.session.BaseSession;
import version_01.util.AttributeKey;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

/**
 * Created by mati on 12/09/16.
 */
public class SslFilter {

    /**
     * A session attribute key that stores underlying {@link SSLSession}
     * for each session.
     */
    public static final AttributeKey SSL_SESSION = new AttributeKey(SslFilter.class, "session");


    private static final AttributeKey SSL_HANDLER = new AttributeKey(SslFilter.class, "handler");


    /** The SslContext used */
    /* No qualifier */final SSLContext sslContext;

    /** A flag used to tell the filter to start the handshake immediately */
    private final boolean autoStart;

    /** A flag used to determinate if the handshake should start immediately */
    private static final boolean START_HANDSHAKE = true;


    private boolean client;

    private boolean needClientAuth;

    private boolean wantClientAuth;

    private String[] enabledCipherSuites;

    private String[] enabledProtocols;

    /**
     * Creates a new SSL filter using the specified {@link SSLContext}.
     * The handshake will start immediately.
     *
     * @param sslContext The SSLContext to use
     */
    public SslFilter(SSLContext sslContext) {
        this(sslContext, START_HANDSHAKE);
    }

    /**
     * Creates a new SSL filter using the specified {@link SSLContext}.
     * If the <tt>autostart</tt> flag is set to <tt>true</tt>, the
     * handshake will start immediately.
     *
     * @param sslContext The SSLContext to use
     * @param autoStart The flag used to tell the filter to start the handshake immediately
     */
    public SslFilter(SSLContext sslContext, boolean autoStart) {
        if (sslContext == null) {
            throw new IllegalArgumentException("sslContext");
        }

        this.sslContext = sslContext;
        this.autoStart = autoStart;
    }

    /**
     * @return <tt>true</tt> if the engine will <em>require</em> client authentication.
     * This option is only useful to engines in the server mode.
     */
    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    /**
     * Configures the engine to <em>require</em> client authentication.
     * This option is only useful for engines in the server mode.
     *
     * @param needClientAuth A flag set when we need to authenticate the client
     */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    /**
     * Returns the underlying {@link SSLSession} for the specified session.
     *
     * @param session The current session
     * @return <tt>null</tt> if no {@link SSLSession} is initialized yet.
     */
    public SSLSession getSslSession(BaseSession session) {
        return (SSLSession) session.getAttribute(SSL_SESSION);
    }


    /**
     * @return <tt>true</tt> if the engine will <em>request</em> client authentication.
     * This option is only useful to engines in the server mode.
     */
    public boolean isWantClientAuth() {
        return wantClientAuth;
    }

    /**
     * Configures the engine to <em>request</em> client authentication.
     * This option is only useful for engines in the server mode.
     *
     * @param wantClientAuth A flag set when we want to check the client authentication
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
}
