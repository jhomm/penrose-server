package org.safehaus.penrose.federation;

import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;
import org.safehaus.penrose.client.PenroseClient;
import org.safehaus.penrose.partition.PartitionClient;
import org.safehaus.penrose.partition.PartitionManagerClient;
import org.safehaus.penrose.module.ModuleClient;
import org.safehaus.penrose.module.ModuleManagerClient;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;

import gnu.getopt.LongOpt;
import gnu.getopt.Getopt;

/**
 * @author Endi Sukma Dewata
 */
public class FederationClient implements FederationMBean {

    public static Logger log = Logger.getLogger(FederationClient.class);

    String federationDomain;

    PenroseClient client;
    PartitionClient partitionClient;
    ModuleClient moduleClient;

    public FederationClient(PenroseClient client, String federationDomain) throws Exception {
        this.client = client;
        this.federationDomain = federationDomain;

        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        partitionClient = partitionManagerClient.getPartitionClient(federationDomain);

        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        moduleClient = moduleManagerClient.getModuleClient(Federation.FEDERATION);
    }

    public String getFederationDomain() {
        return federationDomain;
    }

    public Collection<String> getRepositoryTypes() throws Exception {
        return (Collection<String>)moduleClient.getAttribute("RepositoryTypes");
    }

    public PenroseClient getClient() {
        return client;
    }
    
    public PartitionClient getPartitionClient() {
        return partitionClient;
    }

    public ModuleClient getModuleClient() {
        return moduleClient;
    }

    public ModuleClient getRepositoryModuleClient(String type) throws Exception {
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        return moduleManagerClient.getModuleClient(type);
    }

    public void addRepository(FederationRepositoryConfig repository) throws Exception {
        moduleClient.invoke(
                "addRepository",
                new Object[] { repository },
                new String[] { FederationRepositoryConfig.class.getName() }
        );
    }

    public void updateRepository(FederationRepositoryConfig repository) throws Exception {
        moduleClient.invoke(
                "updateRepository",
                new Object[] { repository },
                new String[] { FederationRepositoryConfig.class.getName() }
        );
    }

    public void removeRepository(String repositoryName) throws Exception {
        moduleClient.invoke(
                "removeRepository",
                new Object[] { repositoryName },
                new String[] { String.class.getName() }
        );
    }

    public FederationConfig getFederationConfig() throws Exception {
        return (FederationConfig)moduleClient.getAttribute("FederationConfig");
    }

    public void setFederationConfig(FederationConfig federationConfig) throws Exception {
        moduleClient.setAttribute("FederationConfig", federationConfig);
    }

    public void load() throws Exception {
        moduleClient.invoke("load");
    }

    public void store() throws Exception {
        moduleClient.invoke("store");
    }

    public void clear() throws Exception {
        moduleClient.invoke("clear");
    }

    public Collection<String> getRepositoryNames() throws Exception {
        return (Collection<String>)moduleClient.getAttribute("RepositoryNames");
    }

    public Collection<String> getRepositoryNames(String type) throws Exception {
        return (Collection<String>)moduleClient.invoke(
                "getRepositoryNames",
                new Object[] { type },
                new String[] { String.class.getName() }
        );
    }

    public Collection<FederationRepositoryConfig> getRepositories() throws Exception {
        return (Collection<FederationRepositoryConfig>)moduleClient.getAttribute("Repositories");
    }

    public Collection<FederationRepositoryConfig> getRepositories(String type) throws Exception {
        return (Collection<FederationRepositoryConfig>)moduleClient.invoke(
                "getRepositories",
                new Object[] { type },
                new String[] { String.class.getName() }
        );
    }

    public FederationRepositoryConfig getRepository(String name) throws Exception {
        return (FederationRepositoryConfig)moduleClient.invoke(
                "getRepository",
                new Object[] { name },
                new String[] { String.class.getName() }
        );
    }

    public Collection<String> getPartitionNames() throws Exception {
        return (Collection)moduleClient.invoke(
                "getPartitionNames",
                new Object[] { },
                new String[] { }
        );
    }

    public Collection<FederationPartitionConfig> getPartitions() throws Exception {
        return (Collection)moduleClient.invoke(
                "getPartitions",
                new Object[] { },
                new String[] { }
        );
    }

    public FederationPartitionConfig getPartition(String name) throws Exception {
        return (FederationPartitionConfig)moduleClient.invoke(
                "getPartition",
                new Object[] { name },
                new String[] { String.class.getName() }
        );
    }

    public void createPartition(String name) throws Exception {
        moduleClient.invoke(
                "createPartition",
                new Object[] { name },
                new String[] { String.class.getName() }
        );
    }

    public void removePartition(String name) throws Exception {
        moduleClient.invoke(
                "removePartition",
                new Object[] { name },
                new String[] { String.class.getName() }
        );
    }

    public void synchronize(String name) throws Exception {
        moduleClient.invoke(
                "synchronize",
                new Object[] { name },
                new String[] { String.class.getName() }
        );
    }

    public static void execute(PenroseClient client, Collection<String> commands) throws Exception {

        Iterator<String> iterator = commands.iterator();
        String command = iterator.next();

        if ("show".equals(command)) {
            String partition = iterator.next();
            FederationClient federationClient = new FederationClient(client, partition);

        } else if ("synchronize".equals(command)) {

            String partition = iterator.next();
            FederationClient federationClient = new FederationClient(client, partition);

            Collection<String> repositoryNames;
            if (iterator.hasNext()) {
                repositoryNames = new ArrayList<String>();
                while (iterator.hasNext()) {
                    String repository = iterator.next();
                    repositoryNames.add(repository);
                }

            } else {
                repositoryNames = federationClient.getRepositoryNames();
            }

            for (String repository : repositoryNames) {
                System.out.println("Synchronizing "+repository+"...");
                federationClient.synchronize(repository);
            }

            System.out.println("Done.");

        } else {
            throw new Exception("Unknown command: "+command);
        }
    }

    public static void showUsage() {
        System.out.println("Usage: org.safehaus.penrose.federation.FederationClient [OPTION]... <command> [arguments]...");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -?, --help         display this help and exit");
        System.out.println("  -P protocol        Penrose JMX protocol");
        System.out.println("  -h host            Penrose server");
        System.out.println("  -p port            Penrose JMX port");
        System.out.println("  -D username        username");
        System.out.println("  -w password        password");
        System.out.println("  -d                 run in debug mode");
        System.out.println("  -v                 run in verbose mode");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  show domains                                          Show all federation domains.");
        System.out.println("  show <domain>                                         Show federation domain.");
        System.out.println("  show partitions in domain <domain>                    Show partitions in this domain.");
        System.out.println("  synchronize partition <partition> in domain <domain>  Synchronize partition in this domain.");
    }

    public static void main(String args[]) throws Exception {

        Level level          = Level.WARN;
        String serverType    = PenroseClient.PENROSE;
        String protocol      = PenroseClient.DEFAULT_PROTOCOL;
        String hostname      = "localhost";
        int port             = PenroseClient.DEFAULT_RMI_PORT;
        int rmiTransportPort = PenroseClient.DEFAULT_RMI_TRANSPORT_PORT;

        String bindDn = null;
        String bindPassword = null;

        LongOpt[] longopts = new LongOpt[1];
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, '?');

        Getopt getopt = new Getopt("FederationClient", args, "-:?dvt:h:p:r:P:D:w:", longopts);

        Collection<String> commands = new ArrayList<String>();
        int c;
        while ((c = getopt.getopt()) != -1) {
            switch (c) {
                case ':':
                case '?':
                    showUsage();
                    System.exit(0);
                    break;
                case 1:
                    commands.add(getopt.getOptarg());
                    break;
                case 'd':
                    level = Level.DEBUG;
                    break;
                case 'v':
                    level = Level.INFO;
                    break;
                case 'P':
                    protocol = getopt.getOptarg();
                    break;
                case 't':
                    serverType = getopt.getOptarg();
                    break;
                case 'h':
                    hostname = getopt.getOptarg();
                    break;
                case 'p':
                    port = Integer.parseInt(getopt.getOptarg());
                    break;
                case 'r':
                    rmiTransportPort = Integer.parseInt(getopt.getOptarg());
                    break;
                case 'D':
                    bindDn = getopt.getOptarg();
                    break;
                case 'w':
                    bindPassword = getopt.getOptarg();
            }
        }

        if (commands.size() == 0) {
            showUsage();
            System.exit(0);
        }

        File penroseHome = new File(System.getProperty("org.safehaus.penrose.client.home"));

        //Logger rootLogger = Logger.getRootLogger();
        //rootLogger.setLevel(Level.OFF);

        Logger logger = Logger.getLogger("org.safehaus.penrose");

        File log4jXml = new File(penroseHome, "conf"+File.separator+"log4j.xml");

        if (level.equals(Level.DEBUG)) {
            logger.setLevel(level);
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%-20C{1} [%4L] %m%n"));
            BasicConfigurator.configure(appender);

        } else if (level.equals(Level.INFO)) {
            logger.setLevel(level);
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("[%d{MM/dd/yyyy HH:mm:ss}] %m%n"));
            BasicConfigurator.configure(appender);

        } else if (log4jXml.exists()) {
            DOMConfigurator.configure(log4jXml.getAbsolutePath());

        } else {
            logger.setLevel(level);
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("[%d{MM/dd/yyyy HH:mm:ss}] %m%n"));
            BasicConfigurator.configure(appender);
        }

        try {
            PenroseClient client = new PenroseClient(
                    serverType,
                    protocol,
                    hostname,
                    port,
                    bindDn,
                    bindPassword
            );

            client.setRmiTransportPort(rmiTransportPort);
            client.connect();

            execute(client, commands);

            client.close();

        } catch (SecurityException e) {
            log.error(e.getMessage());

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
