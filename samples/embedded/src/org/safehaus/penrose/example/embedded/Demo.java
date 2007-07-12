package org.safehaus.penrose.example.embedded;

import org.apache.log4j.*;
import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.config.DefaultPenroseConfig;
import org.safehaus.penrose.PenroseFactory;
import org.safehaus.penrose.Penrose;
import org.safehaus.penrose.ldap.Attributes;
import org.safehaus.penrose.ldap.Attribute;
import org.safehaus.penrose.naming.PenroseContext;
import org.safehaus.penrose.mapping.EntryMapping;
import org.safehaus.penrose.mapping.AttributeMapping;
import org.safehaus.penrose.partition.PartitionManager;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.PartitionConfig;
import org.safehaus.penrose.session.Session;
import org.safehaus.penrose.ldap.SearchResponse;
import org.safehaus.penrose.ldap.SearchResult;

import java.util.Iterator;
import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class Demo {

    Logger log = Logger.getLogger(getClass());

    public Demo() {
        ConsoleAppender appender = new ConsoleAppender(new PatternLayout("[%d{MM/dd/yyyy HH:mm:ss}] %m%n"));
        BasicConfigurator.configure(appender);

        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.OFF);

        Logger logger = Logger.getLogger("org.safehaus.penrose");
        logger.setLevel(Level.WARN);
    }

    public EntryMapping createRootEntry() {

        EntryMapping entryMapping = new EntryMapping();
        entryMapping.setDn("dc=Example,dc=com");

        entryMapping.addObjectClass("dcObject");
        entryMapping.addObjectClass("organization");

        AttributeMapping attribute = new AttributeMapping();
        attribute.setName("dc");
        attribute.setConstant("Example");
        attribute.setRdn(true);

        entryMapping.addAttributeMapping(attribute);

        attribute = new AttributeMapping();
        attribute.setName("o");
        attribute.setConstant("Example");

        entryMapping.addAttributeMapping(attribute);

        return entryMapping;
    }

    public EntryMapping createUsersEntry() {

        EntryMapping entryMapping = new EntryMapping();
        entryMapping.setDn("ou=Users,dc=Example,dc=com");

        entryMapping.addObjectClass("organizationalUnit");

        AttributeMapping attribute = new AttributeMapping();
        attribute.setName("ou");
        attribute.setConstant("Users");
        attribute.setRdn(true);

        entryMapping.addAttributeMapping(attribute);

        return entryMapping;
    }

    public void run() throws Exception {

        log.warn("Creating partition.");

        PartitionConfig partitionConfig = new PartitionConfig();
        partitionConfig.setName("Example");

        Partition partition = new Partition(partitionConfig);

        EntryMapping rootEntry = createRootEntry();
        partition.getMappings().addEntryMapping(rootEntry);

        EntryMapping usersEntry = createUsersEntry();
        partition.getMappings().addEntryMapping(usersEntry);

        log.warn("Configuring Penrose.");

        PenroseConfig penroseConfig = new DefaultPenroseConfig();
        penroseConfig.removePartitionConfig("DEFAULT");

        PenroseFactory penroseFactory = PenroseFactory.getInstance();
        Penrose penrose = penroseFactory.createPenrose(penroseConfig);

        PenroseContext penroseContext = penrose.getPenroseContext();
        PartitionManager partitionManager = penroseContext.getPartitionManager();
        partitionManager.addPartition(partition);

        log.warn("Starting Penrose.");

        penrose.start();

        log.warn("Connecting to Penrose.");

        Session session = penrose.newSession();
        session.bind("uid=admin,ou=system", "secret");

        log.warn("Searching all entries.");

        SearchResponse<SearchResult> response = session.search("dc=Example,dc=com", "(objectClass=*)");

        while (response.hasNext()) {
            SearchResult searchResult = (SearchResult) response.next();
            log.warn("Entry:\n"+toString(searchResult));
        }

        penrose.stop();
    }

    public String toString(SearchResult searchResult) throws Exception {

        StringBuffer sb = new StringBuffer();
        sb.append("dn: "+searchResult.getDn()+"\n");

        Attributes attributes = searchResult.getAttributes();
        for (Iterator i=attributes.getAll().iterator(); i.hasNext(); ) {
            Attribute attribute = (Attribute)i.next();

            String name = attribute.getName();
            Collection values = attribute.getValues();

            for (Iterator j=values.iterator(); j.hasNext(); ) {
                Object value = j.next();
                sb.append(name+": "+value+"\n");
            }
        }

        return sb.toString();
    }

    public static void main(String args[]) throws Exception {
        Demo demo = new Demo();
        demo.run();
    }
}
