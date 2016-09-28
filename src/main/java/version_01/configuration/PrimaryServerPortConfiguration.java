package version_01.configuration;


import version_01.core.conf.PortConfigurationAdapter;

/**
 * Created by mati on 09/09/16.
 * todo: esta clase será un archivo de configuración que el usuario del nodo pueda cambiar desde un txt o algo así..
 */
public class PrimaryServerPortConfiguration extends PortConfigurationAdapter {


    public PrimaryServerPortConfiguration() {
        super(
                ServerPortType.PRIMARY,
                true, // reuse address
                9888, // port
                50,   // backlog
                2,    // ioThreadsCount
                4,    // workersCount
                false // isSecure
        );
    }

    public PrimaryServerPortConfiguration(boolean reuseAddress, int port, int backlog, int ioThreadsCount, int workersCount,boolean isSecure) {
        super(ServerPortType.PRIMARY,reuseAddress, port, backlog, ioThreadsCount, workersCount,isSecure);
    }

}
