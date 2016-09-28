package version_mati_01.util;

import com.sun.corba.se.spi.activation.Server;
import version_mati_01.configuration.CustomerServerPortConfiguration;
import version_mati_01.configuration.NonCustomerServerPortConfiguration;
import version_mati_01.configuration.PrimaryServerPortConfiguration;
import version_mati_01.configuration.ServerPortType;
import version_mati_01.core.conf.ConfigurationManager;
import version_mati_01.core.conf.ServerPortConfiguration;
import version_mati_01.structure.messages.MessageFactory;
import version_mati_01.structure.protos.TestProto3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mati on 25/09/16.
 */
public class ConfigurationsUtil {

    /**
     * Helper method
     *
     * @param serverPortType
     * @return
     */
    public static ServerPortConfiguration buildServerPortConfiguration(ConfigurationManager configurationManager,ServerPortType serverPortType){
        ServerPortConfiguration serverPortConfiguration = null;
        switch (serverPortType){
            case PRIMARY:
                serverPortConfiguration = new PrimaryServerPortConfiguration(
                        configurationManager.getValueBoolean(configurationManager.getKey(serverPortType, ConfigurationManager.REUSE_ADDRESS)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.PORT)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.BACKLOG)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.IO_THREADS_COUNT)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.WORKERS)),
                        configurationManager.getValueBoolean(configurationManager.getKey(serverPortType,ConfigurationManager.TLS))
                        );
                break;
            case CUSTOMER:
                serverPortConfiguration = new CustomerServerPortConfiguration(
                        configurationManager.getValueBoolean(configurationManager.getKey(serverPortType,ConfigurationManager.REUSE_ADDRESS)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.PORT)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.BACKLOG)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.IO_THREADS_COUNT)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.WORKERS)),
                        configurationManager.getValueBoolean(configurationManager.getKey(serverPortType,ConfigurationManager.TLS))
                );
                break;
            case NON_CUSTOMER:
                serverPortConfiguration = new NonCustomerServerPortConfiguration(
                        configurationManager.getValueBoolean(configurationManager.getKey(serverPortType,ConfigurationManager.REUSE_ADDRESS)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.PORT)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.BACKLOG)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.IO_THREADS_COUNT)),
                        configurationManager.getValueInt(configurationManager.getKey(serverPortType,ConfigurationManager.WORKERS)),
                        configurationManager.getValueBoolean(configurationManager.getKey(serverPortType,ConfigurationManager.TLS))

                );
                break;
            default:
                throw new IllegalArgumentException("ServerPortType invalid value..");
        }
        return serverPortConfiguration;
    }

    public static List<TestProto3.ServerRole> getServerRolesConfigurations(Collection<ServerPortConfiguration> c){
        List<TestProto3.ServerRole> roles = new ArrayList<>();
        for (ServerPortConfiguration portConfiguration : c) {
            roles.add(buildServerRoleTypeByConfigurations(portConfiguration));
        }
        return roles;
    }

    public static TestProto3.ServerRole buildServerRoleTypeByConfigurations(ServerPortConfiguration portConfiguration){
        // always is tcp because we don't have UDP ports
        return MessageFactory.buildServerRole(convertToRole(portConfiguration.getPortType()),portConfiguration.getPort(),portConfiguration.isSecure(),true);
    }

    public static TestProto3.ServerRoleType convertToRole(ServerPortType portType){
        TestProto3.ServerRoleType serverRoleType = null;
        switch (portType){
            case CUSTOMER:
                serverRoleType = TestProto3.ServerRoleType.CL_CUSTOMER;
                break;
            case NON_CUSTOMER:
                serverRoleType = TestProto3.ServerRoleType.CL_NON_CUSTOMER;
                break;
            case PRIMARY:
                serverRoleType = TestProto3.ServerRoleType.PRIMARY;
                break;
            default:
                throw new IllegalArgumentException("ServerPortType not implemented");
        }
        return serverRoleType;
    }

}
