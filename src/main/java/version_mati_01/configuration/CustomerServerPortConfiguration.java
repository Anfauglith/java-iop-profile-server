package version_mati_01.configuration;


import version_mati_01.core.conf.PortConfigurationAdapter;

/**
 * Created by mati on 09/09/16.
 * todo: esta clase será un archivo de configuración que el usuario del nodo pueda cambiar desde un txt o algo así..
 */
public class CustomerServerPortConfiguration extends PortConfigurationAdapter {

    public CustomerServerPortConfiguration() {
        super(
                ServerPortType.CUSTOMER,
                true, // reuse address
                9890, // port
                50,   // backlog
                2,    // ioThreadsCount
                6,    // workersCount
                true  // isSecure
        );
    }

    public CustomerServerPortConfiguration(boolean reuseAddress, int port, int backlog, int ioThreadsCount, int workersCount,boolean isSecure) {
        super(ServerPortType.CUSTOMER,reuseAddress, port, backlog, ioThreadsCount, workersCount,isSecure);
    }
}
