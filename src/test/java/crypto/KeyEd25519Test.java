package crypto;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import version_01.structure.logic.crypto.CryptoBytes;
import version_01.structure.logic.crypto.KeyEd25519;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by mati on 09/10/16.
 */
public class KeyEd25519Test {

    static KeyEd25519 keyEd25519;


    /**
     * Key generation test
     */
    @Test
    public void test_create_Ed25519Keys(){
        System.out.println("#########  Setup  ##########");
        long now = System.currentTimeMillis();
        keyEd25519 = KeyEd25519.generateKeys();
        System.out.println(keyEd25519);
        System.out.println("-------------------------");
        System.out.println("publicKey from hex to bytes again: ");
        System.out.println(Arrays.toString(CryptoBytes.fromHexToBytes(keyEd25519.getPublicKeyHex())));
        long finish = System.currentTimeMillis();
        long delta = finish-now;
        System.out.println("Time: "+ TimeUnit.MILLISECONDS.convert(delta,TimeUnit.SECONDS));
        System.out.println("#########  End Setup  ##########");
    }

    /**
     * Sign and verify functions
     *
     * @throws UnsupportedEncodingException
     */
    @Test
    public void test_signAndVerifyMessage_Ed25519() throws UnsupportedEncodingException {
        test_create_Ed25519Keys();
        System.out.println("#########  sign and verify  ##########");
        String message = "hola";
        byte[] signature = keyEd25519.sign(message,keyEd25519.getExpandedPrivateKey());
        System.out.println("Signature: size "+signature.length);
        System.out.println(Arrays.toString(signature));
        System.out.println("------------------------");
        boolean isValid = keyEd25519.verify(signature,message,keyEd25519.getPublicKey());
        System.out.println("Verified method return: "+isValid);
        System.out.println("#########  end sign and verify  ##########");
    }



}
