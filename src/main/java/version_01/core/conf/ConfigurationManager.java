package version_01.core.conf;

import org.apache.commons.configuration.ConfigurationException;
import version_01.configuration.ServerPortType;

import java.io.IOException;

/**
 * Created by mati on 23/09/16.
 */
public interface ConfigurationManager {

    /** Represent the value of PORT */
    public static final String PORT = "port";
    /** Represent the value of reuse address */
    public static final String REUSE_ADDRESS = "reUseAddres";
    /** Represent the value of backlog */
    public static final String BACKLOG = "backlog";
    /** Represent the value of io threads */
    public static final String IO_THREADS_COUNT = "ioThreads";
    /** Represent the value of workers */
    public static final String WORKERS = "workers";
    /** if the port is secure by TLS */
    final String TLS = "tls";

    boolean exist();

    void create() throws IOException, ConfigurationException;

    void load() throws ConfigurationException;

    /**
     * Get the value of a property
     *
     */
    String getValue(String key);
    int getValueInt(String key);
    int[] getValueIntArray(String key);
    boolean getValueBoolean(String key);



    /**
     * Update the value of a property
     */
    void updateValue(String property, String value) throws ConfigurationException;
    void updateValue(String property, String[] value) throws ConfigurationException;
    void updateValue(String property, int value) throws ConfigurationException;
    void updateValue(String property, int[] value) throws ConfigurationException;
    void updateValue(String property, boolean value) throws ConfigurationException;


    String getKey(ServerPortType portType, String configurationValue);
    
}
