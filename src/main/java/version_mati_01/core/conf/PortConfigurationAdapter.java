package version_mati_01.core.conf;

import version_mati_01.configuration.ServerPortType;
import version_mati_01.core.conf.ServerPortConfiguration;

/**
 * Created by mati on 23/09/16.
 */
public class PortConfigurationAdapter implements ServerPortConfiguration {

    /** Server port type */
    private ServerPortType portType;
    /** Reuse address */
    private boolean reuseAddress;
    /** Port */
    private int port;
    /** Backlog */
    private int backlog;
    /** I/O  Threads */
    private int ioThreadsCount;
    /** Workers */
    private int workersCount;
    /** Secure session */
    private boolean isSecure;

    public PortConfigurationAdapter(ServerPortType portType,boolean reuseAddress, int port, int backlog, int ioThreadsCount, int workersCount,boolean isSecure) {
        this.portType = portType;
        this.reuseAddress = reuseAddress;
        this.port = port;
        this.backlog = backlog;
        this.ioThreadsCount = ioThreadsCount;
        this.workersCount = workersCount;
        this.isSecure = isSecure;
    }

    @Override
    public ServerPortType getPortType() {
        return portType;
    }

    @Override
    public int getPort() {
        return port;
    }
    @Override
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public int getBacklog() {
        return backlog;
    }

    @Override
    public int getIoThreadsCount() {
        return ioThreadsCount;
    }

    @Override
    public int getWorkersCount() {
        return workersCount;
    }
}
