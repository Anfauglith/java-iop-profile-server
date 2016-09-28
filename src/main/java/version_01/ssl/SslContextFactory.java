package version_01.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
     * TestContextFactory
     */

    public class SslContextFactory{

        private static String keyStoreFile = "testKeys.jks";

        public static SSLContext buildContext() throws Exception{

            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                KeyStore ts = KeyStore.getInstance("JKS");

                char[] passphrase = "passphrase".toCharArray();

                File file = new File(keyStoreFile);
                ks.load(new FileInputStream(file), passphrase);
                ts.load(new FileInputStream(file), passphrase);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, passphrase);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ts);

                SSLContext sslCtx = SSLContext.getInstance("TLS");

                sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                return sslCtx;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
            throw new Exception("Algo pasó, fijate el log de arriba please..");
        }

    }