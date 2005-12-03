/**
 * Copyright (c) 2000-2005, Identyx Corporation.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.safehaus.penrose.management;

import org.apache.log4j.*;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.*;
import java.io.File;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

public class PenroseClient {

    public Logger log = Logger.getLogger(PenroseClient.class);

    public final static String MBEAN_NAME = "Penrose:type=Penrose";

	public String url;
    public String username;
    public String password;

	public JMXServiceURL address;
	public Map environment;
	public JMXConnector connector;
	public MBeanServerConnection connection;
	public String domain;
	public ObjectName name;
    public String host;
    public int port;

	public PenroseClient(String host, int port, String username, String password) throws Exception {
        this.host = host;
        this.port = port;
        this.url = "service:jmx:rmi:///jndi/rmi://"+host+(port == 0 ? "" : ":"+port)+"/jmx";
        //this.url = "service:jmx:rmi://"+host+(port == 0 ? "" : ":"+port);
		this.username = username;
		this.password = password;
	}

	public void connect() throws Exception {

        log.debug("Connecting to "+host+":"+port+" as "+username+" with password "+password);

		address = new JMXServiceURL(url);

        String[] credentials = new String[2];
        credentials[0] = username;
        credentials[1] = password;

        environment = new HashMap();
        environment.put(JMXConnector.CREDENTIALS, credentials);

		connector = JMXConnectorFactory.connect(address, environment);

		connection = connector.getMBeanServerConnection();
		domain = connection.getDefaultDomain();
		name = new ObjectName(MBEAN_NAME);
	}

    public void close() throws Exception {
        connector.close();
    }

	public Object invoke(String method, Object[] paramValues, String[] paramClassNames) throws Exception {
		return connection.invoke(name, method, paramValues, paramClassNames);
	}

    public String getProductName() throws Exception {
        return (String)connection.getAttribute(name, "ProductName");
    }

    public String getProductVersion() throws Exception {
        return (String)connection.getAttribute(name, "ProductVersion");
    }

    public void start(String serviceName) throws Exception {
        invoke("start",
                new Object[] { serviceName },
                new String[] { String.class.getName() }
        );
    }

    public void stop(String serviceName) throws Exception {
        invoke("stop",
                new Object[] { serviceName },
                new String[] { String.class.getName() }
        );
    }

    public Collection listFiles(String directory) throws Exception {
        return (Collection)invoke("listFiles",
                new Object[] { directory },
                new String[] { String.class.getName() }
        );
    }

    public byte[] download(String filename) throws Exception {
        return (byte[])invoke("download",
                new Object[] { filename },
                new String[] { String.class.getName() }
        );
    }

    public void upload(String filename, byte content[]) throws Exception {
        invoke("upload",
                new Object[] { filename, content },
                new String[] { String.class.getName(), "[B" }
        );
    }

    public Collection getLoggerNames(String path) throws Exception {
        return (Collection)invoke("getLoggerNames",
                new Object[] { path },
                new String[] { String.class.getName() }
        );
    }

    public static void showUsage() {
        System.out.println("Usage: org.safehaus.penrose.management.PenroseClient [OPTION]... <COMMAND>");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  version            get server version");
        System.out.println("  start <service>    start service");
        System.out.println("  stop <service>     stop service");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -?, --help         display this help and exit");
        System.out.println("  -h host            Penrose server");
        System.out.println("  -p port            Penrose JMX port");
        System.out.println("  -D binddn          bind DN");
        System.out.println("  -w password        bind password");
        System.out.println("  -d                 run in debug mode");
        System.out.println("  -v                 run in verbose mode");
    }

    public static void main(String args[]) throws Exception {

        String logLevel = "NORMAL";
        String hostname = "localhost";
        int portNumber = 0;
        String bindDn = null;
        String bindPassword = null;

        LongOpt[] longopts = new LongOpt[1];
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, '?');

        Getopt getopt = new Getopt("PenroseClient", args, "-:?dvh:p:D:w:", longopts);

        Collection parameters = new ArrayList();
        int c;
        while ((c = getopt.getopt()) != -1) {
            switch (c) {
                case ':':
                case '?':
                    showUsage();
                    System.exit(0);
                    break;
                case 1:
                    parameters.add(getopt.getOptarg());
                    break;
                case 'd':
                    logLevel = "DEBUG";
                    break;
                case 'v':
                    logLevel = "VERBOSE";
                    break;
                case 'h':
                    hostname = getopt.getOptarg();
                    break;
                case 'p':
                    portNumber = Integer.parseInt(getopt.getOptarg());
                    break;
                case 'D':
                    bindDn = getopt.getOptarg();
                    break;
                case 'w':
                    bindPassword = getopt.getOptarg();
            }
        }

        if (parameters.size() == 0) {
            showUsage();
            System.exit(0);
        }

        String homeDirectory = System.getProperty("penrose.home");

        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.toLevel("OFF"));

        Logger logger = Logger.getLogger("org.safehaus.penrose");
        File log4jProperties = new File((homeDirectory == null ? "" : homeDirectory+File.separator)+"conf"+File.separator+"log4j.properties");

        if (log4jProperties.exists()) {
            PropertyConfigurator.configure(log4jProperties.getAbsolutePath());

        } else if (logLevel.equals("DEBUG")) {
            logger.setLevel(Level.toLevel("DEBUG"));
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%-20C{1} [%4L] %m%n"));
            BasicConfigurator.configure(appender);

        } else if (logLevel.equals("VERBOSE")) {
            logger.setLevel(Level.toLevel("INFO"));
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("[%d{MM/dd/yyyy HH:mm:ss}] %m%n"));
            BasicConfigurator.configure(appender);

        } else {
            logger.setLevel(Level.toLevel("WARN"));
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("[%d{MM/dd/yyyy HH:mm:ss}] %m%n"));
            BasicConfigurator.configure(appender);
        }

        PenroseClient client = new PenroseClient(hostname, portNumber, bindDn, bindPassword);
        client.connect();

        Iterator iterator = parameters.iterator();
        String command = (String)iterator.next();
        logger.debug("Executing "+command);

        if ("version".equals(command)) {
            String version = client.getProductName()+" "+client.getProductVersion();
            System.out.println(version);

        } else if ("start".equals(command)) {
            String serviceName = (String)iterator.next();
            client.start(serviceName);

        } else if ("stop".equals(command)) {
            String serviceName = (String)iterator.next();
            client.stop(serviceName);
        }

        client.close();
    }
}
