package version_01.structure.filters.ssl.test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Created by mati on 13/09/16.
 */
public class SslContextFactory {

    public static SSLContext createSslContext(char[] passphrase) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        // First initialize the key and trust material.
        KeyStore ksKeys = KeyStore.getInstance("JKS");
        File file = new File("testKeys.jks");
        FileInputStream fileInputStream = new FileInputStream(file);
        ksKeys.load(fileInputStream, passphrase);
        KeyStore ksTrust = KeyStore.getInstance("JKS");
        ksTrust.load(new FileInputStream(/*"testTrust"*/file), passphrase);

        // KeyManager's decide which key material to use.
        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(ksKeys, passphrase);

        // TrustManager's decide whether to allow connections.
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(ksTrust);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }

}
