package version_01.structure.logic.crypto;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Matias Furszyfer on 03/10/16.
 */
public class CryptoImp implements Crypto {


    /**
     *
     * @param data -> UTF-8 string
     * @return -> sha256 digested byte array
     */
    public byte[] sha256(String data){
        byte[] digested = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data.getBytes(Charset.forName("UTF-8")));
            digested = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digested;
    }

    /**
     *
     * @param data -> data to be hashed
     * @return -> sha256 digested byte array
     */
    public byte[] sha256(byte[] data){
        byte[] digested = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data);
            digested = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digested;
    }



    public String hashToString(byte[] hash){
        // convert the byte to hex format
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length()==1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }


}
