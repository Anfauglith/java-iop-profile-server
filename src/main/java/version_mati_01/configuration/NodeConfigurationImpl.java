package version_mati_01.configuration;


/**
 * Created by mati on 09/09/16.
 * todo: esta clase será un archivo de configuración que el usuario del nodo pueda cambiar desde un txt o algo así..
 */
public class NodeConfigurationImpl implements NodeConfiguration {

    /**
     *  Reuse address
     */
    private boolean reuseAddress = true;

    /**
     * Port
     */
    private int port = 9888;

    /**
     * Backlog
     */
    private int backlog = 50;





    private static final NodeConfigurationImpl nodeConfigration = new NodeConfigurationImpl();

    public static NodeConfigurationImpl getInstance(){
        return nodeConfigration;
    }

    private NodeConfigurationImpl() {

    }

    public int getPort() {
        return port;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public int getBacklog() {
        return backlog;
    }
}
