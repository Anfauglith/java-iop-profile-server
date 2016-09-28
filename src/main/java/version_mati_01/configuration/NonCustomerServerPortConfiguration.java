package version_mati_01.configuration;


import version_mati_01.core.conf.PortConfigurationAdapter;

/**
 * Created by mati on 09/09/16.
 * todo: esta clase será un archivo de configuración que el usuario del nodo pueda cambiar desde un txt o algo así..
 */
public class NonCustomerServerPortConfiguration extends PortConfigurationAdapter {

    public NonCustomerServerPortConfiguration() {
        super(
                ServerPortType.NON_CUSTOMER,
                true, // reuse address
                9889, // port
                50,   // backlog
                2,    // ioThreadsCount
                6,    // workersCount
                false // isSecure
        );
    }

    public NonCustomerServerPortConfiguration(boolean reuseAddress, int port, int backlog, int ioThreadsCount, int workersCount,boolean isSecure) {
        super(ServerPortType.NON_CUSTOMER,reuseAddress, port, backlog, ioThreadsCount, workersCount,isSecure);
    }
}
