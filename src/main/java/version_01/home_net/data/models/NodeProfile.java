package version_01.home_net.data.models;

import version_01.structure.logic.crypto.KeyEd25519;

import java.io.Serializable;

/**
 * Created by mati on 11/10/16.
 */
public class NodeProfile implements Serializable {

    /** Serializable id */
    static final long serialVersionUID = 782315253L;

    /** Profile version in http://semver.org/ format. */
    private KeyEd25519 keyPair;
}
