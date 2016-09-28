package version_01.home_net.data.models;

import java.io.Serializable;

/**
 * Created by mati on 25/09/16.
 */
public class IdentityKey implements Serializable {

    static final long serialVersionUID = 782315244L;

    // todo: esto no tiene que ser un pkHash, tiene que ser el id (hash de la pk) pero necesito probar antes..
    private byte[] pkHash;

    public IdentityKey(byte[] number) {
        this.pkHash = number;
    }

    public byte[] getPkHash() {
        return pkHash;
    }

    @Override
    public String toString() {
        return "IdentityKey{" +
                "pkHash='" + pkHash + '\'' +
                '}';
    }
}
