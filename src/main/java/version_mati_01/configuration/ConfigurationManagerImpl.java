package version_mati_01.configuration;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_mati_01.core.conf.ConfigurationManager;
import version_mati_01.util.DirResourcesFilesPathUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by mati on 23/09/16.
 */
public class ConfigurationManagerImpl implements ConfigurationManager{

    /**
     * Represent the logger instance
     */
    private final static Logger LOG = LoggerFactory.getLogger(ConfigurationManagerImpl.class);

    /**
     * Represent the value of DIR_NAME
     */
    public static final String DIR_NAME = DirResourcesFilesPathUtil.createNewFilesPath("");

    /**
     * Represent the value of FILE_NAME
     */
    public static final String FILE_NAME = "iop.conf";



    /**
     * Represent the value of configuration file
     */
    private static final PropertiesConfiguration configuration = new PropertiesConfiguration();

    /**
     * Validate if the file exist
     * @return boolean
     */
    public boolean exist(){

        File file = new File(DIR_NAME+File.separator+FILE_NAME);
        return (file.exists() && !file.isDirectory());

    }

    /**
     * Create a new configuration file
     * @return File
     * @throws IOException
     */
    public void create() throws IOException, ConfigurationException {

        LOG.info("Creating new configuration file...");

        File dir = new File(DIR_NAME);

        if (dir.mkdir()){
            LOG.info("Directory is created!");
        }else{
            LOG.info("Directory already exists.");
        }

        File file = new File(DIR_NAME+File.separator+FILE_NAME);

        System.out.println(("ConfigurationFile path: "+file.getAbsolutePath()));

        if (!file.getParentFile().exists()){
            System.out.println("Creating parents file");
            file.getParentFile().mkdirs();
        }

        if (file.createNewFile()){
            LOG.info("File is created!");
        }else{
            LOG.info("File already exists.");
        }

        LOG.info("Add configuration content");

        PropertiesConfiguration newConfigurationFile = new PropertiesConfiguration();
        newConfigurationFile.setFile(file);
        newConfigurationFile.setHeader("# ***********************************\n# * IOP NODE CONFIGURATION FILE *\n# * www.iop-algo.com                *\n# ***********************************");

        // Default primary port conf
        PrimaryServerPortConfiguration primaryServerPortConfiguration = new PrimaryServerPortConfiguration();

        newConfigurationFile.getLayout().setSeparator("title_"+ ServerPortType.PRIMARY,"----"+ServerPortType.PRIMARY.name()+"----");

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.PRIMARY,PORT), "\n# * SERVER PRIMARY_PORT");
        newConfigurationFile.addProperty(getKey(ServerPortType.PRIMARY,PORT),primaryServerPortConfiguration.getPort());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.PRIMARY,REUSE_ADDRESS), "\n# * REUSE ADDRESS");
        newConfigurationFile.addProperty(getKey(ServerPortType.PRIMARY,REUSE_ADDRESS),primaryServerPortConfiguration.isReuseAddress());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.PRIMARY,BACKLOG), "\n# * PORT BACKLOG");
        newConfigurationFile.addProperty(getKey(ServerPortType.PRIMARY,BACKLOG),primaryServerPortConfiguration.getBacklog());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.PRIMARY,IO_THREADS_COUNT), "\n# * I/O THREADS");
        newConfigurationFile.addProperty(getKey(ServerPortType.PRIMARY,IO_THREADS_COUNT),primaryServerPortConfiguration.getIoThreadsCount());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.PRIMARY,WORKERS), "\n# * WORKERS");
        newConfigurationFile.addProperty(getKey(ServerPortType.PRIMARY,WORKERS),primaryServerPortConfiguration.getWorkersCount());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.PRIMARY,TLS), "\n# * TLS");
        newConfigurationFile.addProperty(getKey(ServerPortType.PRIMARY,TLS),primaryServerPortConfiguration.isSecure());

        // Default non-customer port conf

        // Default customer port conf

        newConfigurationFile.getLayout().setSeparator("title_"+ServerPortType.NON_CUSTOMER,"----"+ServerPortType.NON_CUSTOMER.name()+"----");

        NonCustomerServerPortConfiguration nonCustomerServerPortConfiguration = new NonCustomerServerPortConfiguration();

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.NON_CUSTOMER,PORT), "\n# * SERVER CUSTOMER_PORT");
        newConfigurationFile.addProperty(getKey(ServerPortType.NON_CUSTOMER,PORT),nonCustomerServerPortConfiguration.getPort());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.NON_CUSTOMER,REUSE_ADDRESS), "\n# * REUSE ADDRESS");
        newConfigurationFile.addProperty(getKey(ServerPortType.NON_CUSTOMER,REUSE_ADDRESS),nonCustomerServerPortConfiguration.isReuseAddress());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.NON_CUSTOMER,BACKLOG), "\n# * PORT BACKLOG");
        newConfigurationFile.addProperty(getKey(ServerPortType.NON_CUSTOMER,BACKLOG),nonCustomerServerPortConfiguration.getBacklog());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.NON_CUSTOMER,IO_THREADS_COUNT), "\n# * I/O THREADS");
        newConfigurationFile.addProperty(getKey(ServerPortType.NON_CUSTOMER,IO_THREADS_COUNT),nonCustomerServerPortConfiguration.getIoThreadsCount());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.NON_CUSTOMER,WORKERS), "\n# * WORKERS");
        newConfigurationFile.addProperty(getKey(ServerPortType.NON_CUSTOMER,WORKERS),nonCustomerServerPortConfiguration.getWorkersCount());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.NON_CUSTOMER,TLS), "\n# * TLS");
        newConfigurationFile.addProperty(getKey(ServerPortType.NON_CUSTOMER,TLS),nonCustomerServerPortConfiguration.isSecure());


        // Default customer port conf

        newConfigurationFile.getLayout().setSeparator("title_"+ServerPortType.CUSTOMER,"----"+ServerPortType.CUSTOMER.name()+"----");

        CustomerServerPortConfiguration customerServerPortConfiguration = new CustomerServerPortConfiguration();

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.CUSTOMER,PORT), "\n# * SERVER CUSTOMER_PORT");
        newConfigurationFile.addProperty(getKey(ServerPortType.CUSTOMER,PORT),customerServerPortConfiguration.getPort());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.CUSTOMER,REUSE_ADDRESS), "\n# * REUSE ADDRESS");
        newConfigurationFile.addProperty(getKey(ServerPortType.CUSTOMER,REUSE_ADDRESS),customerServerPortConfiguration.isReuseAddress());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.CUSTOMER,BACKLOG), "\n# * PORT BACKLOG");
        newConfigurationFile.addProperty(getKey(ServerPortType.CUSTOMER,BACKLOG),customerServerPortConfiguration.getBacklog());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.CUSTOMER,IO_THREADS_COUNT), "\n# * I/O THREADS");
        newConfigurationFile.addProperty(getKey(ServerPortType.CUSTOMER,IO_THREADS_COUNT),customerServerPortConfiguration.getIoThreadsCount());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.CUSTOMER,WORKERS), "\n# * WORKERS");
        newConfigurationFile.addProperty(getKey(ServerPortType.CUSTOMER,WORKERS),customerServerPortConfiguration.getWorkersCount());

        newConfigurationFile.getLayout().setComment(getKey(ServerPortType.CUSTOMER,TLS), "\n# * TLS");
        newConfigurationFile.addProperty(getKey(ServerPortType.CUSTOMER,TLS),customerServerPortConfiguration.isSecure());


        newConfigurationFile.save();

        LOG.info("Setup configuration file is complete!");
        LOG.info("Configuration file path = "+file.getAbsolutePath());
    }

    /**
     * Load the content of the configuration file
     * @throws ConfigurationException
     */
    public void load() throws ConfigurationException {
        LOG.info("Loading configuration...");
        configuration.setFileName(DIR_NAME+File.separator+FILE_NAME);
        configuration.load();
    }

    /**
     * Get the value of a properties
     *
     * @param key
     * @return String
     */
    public String getValue(String key){
        return configuration.getString(key);
    }
    public int getValueInt(String key){
        return configuration.getInt(key);
    }
    public int[] getValueIntArray(String key){
        String[] valuesStr = configuration.getStringArray(key);
        int[] values = new int[valuesStr.length];
        for (int i=0;i<values.length;i++){
            values[i] = Integer.parseInt(valuesStr[i]);
        }
        return values;
    }

    @Override
    public boolean getValueBoolean(String key) {
        return configuration.getBoolean(key);
    }


    /**
     * Update the value of a property
     * @param property
     * @param value
     */
    public void updateValue(String property, String value) throws ConfigurationException {
        configuration.setProperty(property, value);
        configuration.save();
    }

    @Override
    public void updateValue(String property, String[] value) throws ConfigurationException {
        configuration.setProperty(property, value);
        configuration.save();
    }

    @Override
    public void updateValue(String property, int value) throws ConfigurationException {
        configuration.setProperty(property, value);
        configuration.save();
    }

    @Override
    public void updateValue(String property, int[] value) throws ConfigurationException {
        configuration.setProperty(property, value);
        configuration.save();
    }

    @Override
    public void updateValue(String property, boolean value) throws ConfigurationException {

    }

    public String getKey(ServerPortType portType, String configurationValue){
        return joinWithUnder(portType,configurationValue);
    }

    private static String joinWithUnder(Object value, Object value2){
        return value+"_"+value2;
    }

}
