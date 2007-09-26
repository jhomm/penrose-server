/**
 * Copyright (c) 2000-2006, Identyx Corporation.
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
package org.safehaus.penrose.config;

import java.util.*;

import org.safehaus.penrose.interpreter.InterpreterConfig;
import org.safehaus.penrose.interpreter.DefaultInterpreter;
import org.safehaus.penrose.adapter.AdapterConfig;
import org.safehaus.penrose.schema.SchemaConfig;
import org.safehaus.penrose.user.UserConfig;
import org.safehaus.penrose.session.SessionConfig;
import org.safehaus.penrose.ldap.DN;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Endi S. Dewata
 */
public class PenroseConfig implements PenroseConfigMBean, Cloneable {

    public Logger log = LoggerFactory.getLogger(getClass());

    private Map<String,String> systemProperties              = new LinkedHashMap<String,String>();
    private Map<String,String> properties                    = new LinkedHashMap<String,String>();

    private Map<String,SchemaConfig>      schemaConfigs      = new LinkedHashMap<String,SchemaConfig>();
    private Map<String,AdapterConfig>     adapterConfigs     = new LinkedHashMap<String,AdapterConfig>();
    private Map<String,InterpreterConfig> interpreterConfigs = new LinkedHashMap<String,InterpreterConfig>();

    private SessionConfig   sessionConfig;

    private UserConfig rootUserConfig;

    public PenroseConfig() {

        sessionConfig = new SessionConfig();

        rootUserConfig = new UserConfig("uid=admin,ou=system", "secret");

        init();
    }

    public void init() {
        addInterpreterConfig(new InterpreterConfig("DEFAULT", DefaultInterpreter.class.getName()));
    }

    public String getSystemProperty(String name) {
        return systemProperties.get(name);
    }

    public Map<String,String> getSystemProperties() {
        return systemProperties;
    }

    public Collection<String> getSystemPropertyNames() {
        return systemProperties.keySet();
    }

    public void setSystemProperty(String name, String value) {
        systemProperties.put(name, value);
    }

    public String removeSystemProperty(String name) {
        return systemProperties.remove(name);
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public Map getProperties() {
        return properties;
    }

    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    public String removeProperty(String name) {
        return properties.remove(name);
    }

    public Collection<InterpreterConfig> getInterpreterConfigs() {
        return interpreterConfigs.values();
    }

    public void addInterpreterConfig(InterpreterConfig interpreterConfig) {
        interpreterConfigs.put(interpreterConfig.getName(), interpreterConfig);
    }

    public Collection<AdapterConfig> getAdapterConfigs() {
        return adapterConfigs.values();
    }

    public AdapterConfig getAdapterConfig(String name) {
        return adapterConfigs.get(name);
    }

    public void addAdapterConfig(AdapterConfig adapter) {
        adapterConfigs.put(adapter.getName(), adapter);
    }

    public Collection<String> getAdapterNames() {
        return adapterConfigs.keySet();
    }

    public void addSchemaConfig(SchemaConfig schemaConfig) {
        schemaConfigs.put(schemaConfig.getName(), schemaConfig);
    }

    public SchemaConfig getSchemaConfig(String name) {
        return schemaConfigs.get(name);
    }

    public Collection<SchemaConfig> getSchemaConfigs() {
        return schemaConfigs.values();
    }

    public Collection<String> getSchemaNames() {
        return schemaConfigs.keySet();
    }

    public SchemaConfig removeSchemaConfig(String name) {
        return schemaConfigs.remove(name);
    }

    public UserConfig getRootUserConfig() {
        return rootUserConfig;
    }

    public void setRootUserConfig(UserConfig rootUserConfig) {
        this.rootUserConfig = rootUserConfig;
    }

    public DN getRootDn() {
        return rootUserConfig.getDn();
    }

    public void setRootDn(String rootDn) {
        rootUserConfig.setDn(rootDn);
    }

    public void setRootDn(DN rootDn) {
        rootUserConfig.setDn(rootDn);
    }

    public byte[] getRootPassword() {
        return rootUserConfig.getPassword();
    }

    public void setRootPassword(String rootPassword) {
        rootUserConfig.setPassword(rootPassword);
    }

    public void setRootPassword(byte[] rootPassword) {
        rootUserConfig.setPassword(rootPassword);
    }

    public SessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public void setSessionConfig(SessionConfig sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    public int hashCode() {
        return (systemProperties == null ? 0 : systemProperties.hashCode()) +
                (properties == null ? 0 : properties.hashCode()) +
                (schemaConfigs == null ? 0 : schemaConfigs.hashCode()) +
                (adapterConfigs == null ? 0 : adapterConfigs.hashCode()) +
                (interpreterConfigs == null ? 0 : interpreterConfigs.hashCode()) +
                (sessionConfig == null ? 0 : sessionConfig.hashCode()) +
                (rootUserConfig == null ? 0 : rootUserConfig.hashCode());
    }

    boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null) return true;
        if (o1 != null) return o1.equals(o2);
        return o2.equals(o1);
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null) return false;
        if (object.getClass() != this.getClass()) return false;

        PenroseConfig penroseConfig = (PenroseConfig)object;

        if (!equals(systemProperties, penroseConfig.systemProperties)) return false;
        if (!equals(properties, penroseConfig.properties)) return false;

        if (!equals(schemaConfigs, penroseConfig.schemaConfigs)) return false;
        if (!equals(adapterConfigs, penroseConfig.adapterConfigs)) return false;
        if (!equals(interpreterConfigs, penroseConfig.interpreterConfigs)) return false;

        if (!equals(sessionConfig, penroseConfig.sessionConfig)) return false;

        if (!equals(rootUserConfig, penroseConfig.rootUserConfig)) return false;

        return true;
    }

    public void copy(PenroseConfig penroseConfig) throws CloneNotSupportedException {

        systemProperties = new LinkedHashMap<String,String>();
        systemProperties.putAll(penroseConfig.systemProperties);

        properties = new LinkedHashMap<String,String>();
        properties.putAll(penroseConfig.properties);

        schemaConfigs = new LinkedHashMap<String,SchemaConfig>();
        for (SchemaConfig schemaConfig : penroseConfig.schemaConfigs.values()) {
            addSchemaConfig((SchemaConfig) schemaConfig.clone());
        }

        adapterConfigs = new LinkedHashMap<String,AdapterConfig>();
        for (AdapterConfig adapterConfig : penroseConfig.adapterConfigs.values()) {
            addAdapterConfig((AdapterConfig) adapterConfig.clone());
        }

        interpreterConfigs = new LinkedHashMap<String,InterpreterConfig>();
        for (InterpreterConfig interpreterConfig : penroseConfig.interpreterConfigs.values()) {
            addInterpreterConfig((InterpreterConfig) interpreterConfig.clone());
        }

        sessionConfig = (SessionConfig)penroseConfig.sessionConfig.clone();

        rootUserConfig = (UserConfig)penroseConfig.rootUserConfig.clone();
    }

    public void clear() {
        systemProperties.clear();
        properties.clear();
        schemaConfigs.clear();
        adapterConfigs.clear();
        interpreterConfigs.clear();

        init();
    }

    public Object clone() throws CloneNotSupportedException {
        PenroseConfig penroseConfig = (PenroseConfig)super.clone();
        penroseConfig.copy(this);
        return penroseConfig;
    }
}
