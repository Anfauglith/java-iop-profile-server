package version_01.home_net.data.models;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by mati on 25/09/16.
 *
 * Database representation of IoP profile
 */
public class IdentityData implements Serializable {

    /** Serializable id */
    static final long serialVersionUID = 782315253L;

    /** Identity identifier is SHA1 hash of identity's public key */
    private byte[] identityId;
    /** Cryptographic public key that represent the identity */
    private byte[] publicKey;
    /** Profile version in http://semver.org/ format. */
    private byte[] version;
    /** User defined profile name */
    private String name;
    /** Profile type. */
    private String type;
    /** User defined profile picture. */
    private byte[] picture;
    /** Encoded representation of the user's initial GPS location. */
    private long initialLocationEncoded;
    /** User defined extra data that serve for satisfying search queries in HomeNet. */
    private String extraData;
    /**  */
    private boolean isOnline;

    public IdentityData(byte[] identityId, byte[] publicKey, byte[] version, String name, String type, byte[] picture, long initialLocationEncoded, String extraData) {
        this.identityId = identityId;
        this.publicKey = publicKey;
        this.version = version;
        this.name = name;
        this.type = type;
        this.picture = picture;
        this.initialLocationEncoded = initialLocationEncoded;
        this.extraData = extraData;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public byte[] getIdentityId() {
        return identityId;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public byte[] getPicture() {
        return picture;
    }

    public long getInitialLocationEncoded() {
        return initialLocationEncoded;
    }

    public String getExtraData() {
        return extraData;
    }

    public boolean isOnline() {
        return isOnline;
    }

    @Override
    public String toString() {
        return "IdentityData{" +
                "identityId=" + Arrays.toString(identityId) +
                ", publicKey=" + Arrays.toString(publicKey) +
                ", version=" + Arrays.toString(version) +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", picture=" + Arrays.toString(picture) +
                ", initialLocationEncoded=" + initialLocationEncoded +
                ", extraData='" + extraData + '\'' +
                '}';
    }
}
