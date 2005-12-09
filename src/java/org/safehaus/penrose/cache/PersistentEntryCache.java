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
package org.safehaus.penrose.cache;

import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.util.PasswordUtil;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.FieldConfig;
import org.safehaus.penrose.partition.SourceConfig;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.connector.ConnectionManager;
import org.safehaus.penrose.interpreter.Interpreter;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Endi S. Dewata
 */
public class PersistentEntryCache extends EntryCache {

    Partition partition;
    int mappingId;

    ConnectionManager connectionManager;
    String jdbcConnectionName;
    String jndiConnectionName;

    public void init() throws Exception {
        super.init();

        //log.debug("-------------------------------------------------------------------------------");
        //log.debug("Initializing PersistentEngineCache:");

        connectionManager = engine.getConnectionManager();
        jdbcConnectionName = getParameter("jdbcConnection");
        jndiConnectionName = getParameter("jndiConnection");

        partition = engine.getPartitionManager().getPartition(entryMapping);

        mappingId = getMappingId();
        if (mappingId == -1) {
            createMappingsTable();
            mappingId = getMappingId();
        }
        if (mappingId == 0) {
            addMapping();
            mappingId = getMappingId();
        }
    }

    public Connection getJDBCConnection() throws Exception {
        return (Connection)connectionManager.openConnection(jdbcConnectionName);
    }

    public DirContext getJNDIConnection() throws Exception {
        return (DirContext)connectionManager.openConnection(jndiConnectionName);
    }

    public void create() throws Exception {

        String dn = entryMapping.getDn();
        log.debug("Entry "+dn+" ("+mappingId+")");

        createEntriesTable();

        Collection attributeMappings = entryMapping.getAttributeMappings();
        for (Iterator i=attributeMappings.iterator(); i.hasNext(); ) {
            AttributeMapping attributeMapping = (AttributeMapping)i.next();
            createAttributeTable(attributeMapping);
        }

        Collection sources = partition.getEffectiveSources(entryMapping);

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();

            SourceConfig sourceConfig = partition.getSourceConfig(sourceMapping.getSourceName());

            Collection fields = sourceConfig.getFieldConfigs();
            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                FieldConfig fieldConfig = (FieldConfig)j.next();
                createFieldTable(sourceMapping, fieldConfig);
            }
        }

        if (!partition.isDynamic(entryMapping)) {
            Interpreter interpreter = engine.getInterpreterFactory().newInstance();
            AttributeValues attributeValues = engine.computeAttributeValues(entryMapping, interpreter);
            interpreter.clear();

            Entry entry = new Entry(dn, entryMapping, attributeValues);
            Row rdn = entry.getRdn();

            put(rdn, entry);
        }

    }

    public void createMappingsTable() throws Exception {
        String sql = "create table penrose_mappings (id integer auto_increment, dn varchar(255) unique, primary key (id))";

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public int getMappingId() throws Exception {
        String dn = entryMapping.getDn();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        int id = 0;

        try {
            String sql = "select id from penrose_mappings where dn=?";
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
                log.debug(Formatter.displayLine("Parameters: dn = "+dn, 80));
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.setObject(1, dn);

            rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1);
            }

            return id;

        } catch (Exception e) {
            log.error(e.getMessage());
            return -1;

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void addMapping() throws Exception {
        String dn = entryMapping.getDn();

        Connection con = null;
        PreparedStatement ps = null;

        try {
            String sql = "insert into penrose_mappings values (null, ?)";
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.setObject(1, dn);

            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public int getEntryId(String dn) throws Exception {

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        int id = 0;

        try {
            String sql = "select id from penrose_"+mappingId+"_entries where dn=?";
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
                log.debug(Formatter.displayLine("Parameters: dn = "+dn, 80));
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.setObject(1, dn);

            rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1);
            }

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displayLine("Results: id = "+id, 80));
                log.debug(Formatter.displaySeparator(80));
            }

            return id;

        } catch (Exception e) {
            log.error(e.getMessage());
            return -1;

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void addEntry(String dn) throws Exception {

        Connection con = null;
        PreparedStatement ps = null;

        try {
            String sql = "insert into penrose_"+mappingId+"_entries values (null, ?)";
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.setObject(1, dn);

            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void removeEntry(int entryId) throws Exception {

        Connection con = null;
        PreparedStatement ps = null;

        try {
            String sql = "delete from penrose_"+mappingId+"_entries where id=?";
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.setInt(1, entryId);

            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void createEntriesTable() throws Exception {

        String tableName = "penrose_"+mappingId+"_entries";

        StringBuffer sb = new StringBuffer();
        sb.append("create table ");
        sb.append(tableName);
        sb.append(" (id integer auto_increment, dn varchar(255) unique, primary key (id))");

        String sql = sb.toString();

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void createAttributeTable(AttributeMapping attributeMapping) throws Exception {

        String tableName = "penrose_"+mappingId+"_attribute_"+attributeMapping.getName();

        StringBuffer sb = new StringBuffer();
        sb.append("create table ");
        sb.append(tableName);
        sb.append(" (id integer, ");
        sb.append(attributeMapping.getName());
        sb.append(" ");
        sb.append(getColumnTypeDeclaration(attributeMapping));
        sb.append(", primary key (id, ");
        sb.append(attributeMapping.getName());
        sb.append("))");

        String sql = sb.toString();

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public String getColumnTypeDeclaration(AttributeMapping attributeMapping) {
        StringBuffer sb = new StringBuffer();
        sb.append(attributeMapping.getType());

        if ("VARCHAR".equals(attributeMapping.getType()) && attributeMapping.getLength() > 0) {
            sb.append("(");
            sb.append(attributeMapping.getLength());
            sb.append(")");
        }

        return sb.toString();
    }

    public String getColumnTypeDeclaration(FieldConfig fieldConfig) {
        StringBuffer sb = new StringBuffer();
        sb.append(fieldConfig.getType());

        if ("VARCHAR".equals(fieldConfig.getType()) && fieldConfig.getLength() > 0) {
            sb.append("(");
            sb.append(fieldConfig.getLength());
            sb.append(")");
        }

        return sb.toString();
    }

    public void createFieldTable(SourceMapping sourceMapping, FieldConfig fieldConfig) throws Exception {

        String tableName = "penrose_"+mappingId+"_field_"+sourceMapping.getName()+"_"+fieldConfig.getName();

        StringBuffer sb = new StringBuffer();
        sb.append("create table ");
        sb.append(tableName);
        sb.append(" (id integer, value ");
        sb.append(getColumnTypeDeclaration(fieldConfig));
        sb.append(", primary key (id, value))");

        String sql = sb.toString();

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void clean() throws Exception {
        if (!partition.isDynamic(entryMapping)) return;

        String dn = entryMapping.getDn();
        Row rdn = Entry.getRdn(dn);
        remove(null);

    }

    public void drop() throws Exception {
        if (!partition.isDynamic(entryMapping)) {
            String dn = entryMapping.getDn();
            Row rdn = Entry.getRdn(dn);
            remove(rdn);
        }

        Collection sources = partition.getEffectiveSources(entryMapping);

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();
            dropEntrySourceTable(sourceMapping);
        }

        Collection attributeDefinitions = entryMapping.getAttributeMappings();
        for (Iterator i=attributeDefinitions.iterator(); i.hasNext(); ) {
            AttributeMapping attributeMapping = (AttributeMapping)i.next();
            dropAttributeTable(attributeMapping);
        }

        dropEntriesTable();
    }

    public void dropEntrySourceTable(SourceMapping sourceMapping) throws Exception {

        SourceConfig sourceConfig = partition.getSourceConfig(sourceMapping.getSourceName());

        Collection fields = sourceConfig.getFieldConfigs();
        for (Iterator i=fields.iterator(); i.hasNext(); ) {
            FieldConfig fieldConfig = (FieldConfig)i.next();
            dropFieldTable(sourceMapping, sourceConfig, fieldConfig);
        }
    }

    public void dropFieldTable(SourceMapping sourceMapping, SourceConfig sourceConfig, FieldConfig fieldConfig) throws Exception {

        String tableName = "penrose_"+mappingId+"_field_"+sourceMapping.getName()+"_"+fieldConfig.getName();

        String sql = "drop table "+tableName;

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void dropEntriesTable() throws Exception {

        String tableName = "penrose_"+mappingId+"_entries";

        String sql = "drop table "+tableName;

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void dropAttributeTable(AttributeMapping attributeMapping) throws Exception {

        String tableName = "penrose_"+mappingId+"_attribute_"+attributeMapping.getName();

        String sql = "drop table "+tableName;

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);
            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void load() throws Exception {

        if (!partition.isDynamic(entryMapping)) return;

        Collection entries = partition.getChildren(entryMapping);
        load(entries);
    }

    public void load(Collection entries) throws Exception {
        for (Iterator i = entries.iterator(); i.hasNext();) {
            EntryMapping ed = (EntryMapping) i.next();

            engine.search(null, new AttributeValues(), ed, null, null);

            //Collection children = config.getChildren(ed);
            //load(children);
        }
    }

    public Object get(Object pk) throws Exception {
        Row rdn = (Row)pk;
        String dn = rdn+","+parentDn;
        Entry entry = null;

        DirContext ctx = null;

        try {
            log.debug("Getting "+dn);
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.OBJECT_SCOPE);

            ctx = getJNDIConnection();
            NamingEnumeration ne = ctx.search(dn, "(objectClass=*)", sc);

            if (!ne.hasMore()) return null;

            SearchResult sr = (SearchResult)ne.next();
            log.debug("Found:");

            Attributes attributes = sr.getAttributes();
            AttributeValues attributeValues = new AttributeValues();
            for (NamingEnumeration ne2 = attributes.getAll(); ne2.hasMore(); ) {
                Attribute attribute = (Attribute)ne2.next();
                String name = attribute.getID();

                for (NamingEnumeration ne3 = attribute.getAll(); ne3.hasMore(); ) {
                    Object value = ne3.next();
                    log.debug(" - "+name+": "+value);
                    attributeValues.add(name, value);
                }
            }

            entry = new Entry(dn, entryMapping, attributeValues);

            int entryId = getEntryId(dn);

            AttributeValues sourceValues = entry.getSourceValues();
            Collection sources = partition.getEffectiveSources(entryMapping);

            for (Iterator i=sources.iterator(); i.hasNext(); ) {
                SourceMapping sourceMapping = (SourceMapping)i.next();

                SourceConfig sourceConfig = partition.getSourceConfig(sourceMapping.getSourceName());

                Collection fields = sourceConfig.getFieldConfigs();
                for (Iterator j=fields.iterator(); j.hasNext(); ) {
                    FieldConfig fieldConfig = (FieldConfig)j.next();

                    Collection values = getField(sourceMapping, fieldConfig, entryId);
                    log.debug(" - "+sourceMapping.getName()+"."+fieldConfig.getName()+": "+values);

                    sourceValues.set(sourceMapping.getName()+"."+fieldConfig.getName(), values);
                }
            }

        } catch (NamingException e) {
            log.error(e.getMessage());

        } finally {
            if (ctx != null) try { ctx.close(); } catch (Exception e) {}
        }

        return entry;
    }

    public Collection search(Filter filter) throws Exception {
        Collection results = new ArrayList();
        DirContext ctx = null;

        try {
            log.debug("Searching "+parentDn+" with filter "+filter);
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            sc.setReturningAttributes(new String[] { "dn" });

            ctx = getJNDIConnection();
            String ldapFilter = filter == null ? "(objectClass=*)" : filter.toString();
            NamingEnumeration ne = ctx.search(parentDn, ldapFilter, sc);

            while (ne.hasMore()) {
                SearchResult sr = (SearchResult)ne.next();
                String dn = sr.getName()+","+parentDn;

                log.debug(" - "+dn);
                results.add(dn);
            }

        } catch (NamingException e) {
            log.error(e.getMessage());

        } finally {
            if (ctx != null) try { ctx.close(); } catch (Exception e) {}
        }

        return results;
    }

    public Map getExpired() throws Exception {
        Map results = new TreeMap();
        return results;
    }

    public Map search(Collection filters) throws Exception {

        Map values = new TreeMap();

        return values;
    }

    public void put(Object key, Object object) throws Exception {

        Entry entry = (Entry)object;
        Row rdn = entry.getRdn();
        String dn = entry.getDn();

        log.debug("Storing "+dn);

        AttributeValues attributeValues = entry.getAttributeValues();
        Attributes attrs = new BasicAttributes();

        for (Iterator i=attributeValues.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Collection values = attributeValues.get(name);
            if (values.isEmpty()) continue;

            Attribute attr = new BasicAttribute(name);
            for (Iterator j=values.iterator(); j.hasNext(); ) {
                Object value = j.next();
                log.debug(" - "+name+": "+value);

                if ("unicodePwd".equals(name)) {
                    attr.add(PasswordUtil.toUnicodePassword(value.toString()));
                } else {
                    attr.add(value.toString());
                }
            }
            attrs.put(attr);
        }

        DirContext ctx = null;
        try {
            ctx = getJNDIConnection();
            ctx.createSubcontext(dn, attrs);
        } catch (NamingException e) {
            log.error(e.getMessage());

        } finally {
            if (ctx != null) try { ctx.close(); } catch (Exception e) {}
        }

        int entryId = getEntryId(dn);
        if (entryId == 0) {
            addEntry(dn);
            entryId = getEntryId(dn);
        }

        Collection attributeDefinitions = entryMapping.getAttributeMappings();
        for (Iterator i=attributeDefinitions.iterator(); i.hasNext(); ) {
            AttributeMapping attributeDefinition = (AttributeMapping)i.next();

            deleteAttribute(attributeDefinition, entryId);

            Collection values = attributeValues.get(attributeDefinition.getName());
            for (Iterator j=values.iterator(); j.hasNext(); ) {
                Object value = j.next();
                insertAttribute(attributeDefinition, entryId, value);
            }
        }

        AttributeValues sourceValues = entry.getSourceValues();
        Collection sources = partition.getEffectiveSources(entryMapping);

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();

            SourceConfig sourceConfig = partition.getSourceConfig(sourceMapping.getSourceName());

            Collection fields = sourceConfig.getFieldConfigs();
            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                FieldConfig fieldConfig = (FieldConfig)j.next();

                deleteField(sourceMapping, fieldConfig, entryId);

                Collection values = sourceValues.get(sourceMapping.getName()+"."+fieldConfig.getName());
                if (values == null) continue;
                
                for (Iterator k=values.iterator(); k.hasNext(); ) {
                    Object value = k.next();
                    insertField(sourceMapping, fieldConfig, entryId, value);
                }
            }
        }
    }

    public void insertAttribute(
            AttributeMapping attributeMapping,
            int entryId,
            Object value
            ) throws Exception {

        String tableName = "penrose_"+mappingId+"_attribute_"+attributeMapping.getName();

        StringBuffer sb = new StringBuffer();
        sb.append("insert into ");
        sb.append(tableName);
        sb.append(" values (?, ?)");

        String sql = sb.toString();

        Collection parameters = new ArrayList();
        parameters.add(new Integer(entryId));
        parameters.add(value);

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);

            int counter = 1;
            for (Iterator i=parameters.iterator(); i.hasNext(); counter++) {
                Object v = i.next();
                ps.setObject(counter, v);
            }

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displayLine("Parameters:", 80));
                counter = 1;
                for (Iterator i=parameters.iterator(); i.hasNext(); counter++) {
                    Object v = i.next();
                    log.debug(Formatter.displayLine(" - "+counter+" = "+v, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void insertField(
            SourceMapping sourceMapping,
            FieldConfig fieldConfig,
            int entryId,
            Object value
            ) throws Exception {

        String tableName = "penrose_"+mappingId+"_field_"+sourceMapping.getName()+"_"+fieldConfig.getName();

        StringBuffer sb = new StringBuffer();
        sb.append("insert into ");
        sb.append(tableName);
        sb.append(" values (?, ?)");

        String sql = sb.toString();

        Collection parameters = new ArrayList();
        parameters.add(new Integer(entryId));
        parameters.add(value);

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);

            int counter = 1;
            for (Iterator i=parameters.iterator(); i.hasNext(); counter++) {
                Object v = i.next();
                ps.setObject(counter, v);
                log.debug(" "+counter+" = "+v);
            }

            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public Collection getField(SourceMapping sourceMapping, FieldConfig fieldConfig, int entryId) throws Exception {

        String tableName = "penrose_"+mappingId+"_field_"+sourceMapping.getName()+"_"+fieldConfig.getName();

        StringBuffer sb = new StringBuffer();
        sb.append("select value from ");
        sb.append(tableName);
        sb.append(" where id=?");

        String sql = sb.toString();

        Collection parameters = new ArrayList();
        parameters.add(new Integer(entryId));

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        Collection values = new ArrayList();

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);

            int counter = 1;
            for (Iterator i=parameters.iterator(); i.hasNext(); counter++) {
                Object param = i.next();
                ps.setObject(counter, param);
            }

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displayLine("Parameters: id = "+entryId, 80));
                log.debug(Formatter.displaySeparator(80));
            }

            rs = ps.executeQuery();

            while (rs.next()) {
                Object value = rs.getObject(1);
                values.add(value);
            }

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displayLine("Results: value = "+values, 80));
                log.debug(Formatter.displaySeparator(80));
            }

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }

        return values;
    }

    public void remove(Object key) throws Exception {
        Row rdn = (Row)key;

        String dn;
        if (rdn == null && parentDn != null) {
            dn = parentDn;
        } else {
            dn = parentDn == null ? entryMapping.getDn() : rdn+","+parentDn;
        }

        DirContext ctx = null;
        try {
            log.debug("Removing "+dn);
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setReturningAttributes(new String[] { "dn" });

            ctx = getJNDIConnection();
            NamingEnumeration ne = ctx.search(dn, "(objectClass=*)", sc);

            ArrayList dns = new ArrayList();
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult)ne.next();
                String name = sr.getName();
                if (rdn == null && "".equals(name)) continue;
                String childDn = "".equals(name) ? dn : name+","+dn;
                dns.add(0, childDn);
            }

            for (Iterator i=dns.iterator(); i.hasNext(); ) {
                String childDn = (String)i.next();
                log.debug(" - "+childDn);

                ctx.destroySubcontext(childDn);
            }

        } catch (NamingException e) {
            log.error(e.getMessage());

        } finally {
            if (ctx != null) try { ctx.close(); } catch (Exception e) {}
        }

        Collection sources = partition.getEffectiveSources(entryMapping);

        int entryId = getEntryId(dn);

        Collection attributeDefinitions = entryMapping.getAttributeMappings();
        for (Iterator i=attributeDefinitions.iterator(); i.hasNext(); ) {
            AttributeMapping attributeDefinition = (AttributeMapping)i.next();

            deleteAttribute(attributeDefinition, entryId);
        }

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();

            SourceConfig sourceConfig = partition.getSourceConfig(sourceMapping.getSourceName());

            Collection fields = sourceConfig.getFieldConfigs();
            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                FieldConfig fieldConfig = (FieldConfig)j.next();
                deleteField(sourceMapping, fieldConfig, entryId);
            }
        }

        removeEntry(entryId);
    }

    public void deleteAttribute(
            AttributeMapping attributeMapping,
            int entryId) throws Exception {

        String tableName = "penrose_"+mappingId+"_attribute_"+attributeMapping.getName();

        Collection parameters = new ArrayList();

        StringBuffer sb = new StringBuffer();
        sb.append("delete from ");
        sb.append(tableName);

        if (entryId > 0) {
            sb.append(" where id=?");
            parameters.add(new Integer(entryId));
        }

        String sql = sb.toString();

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);

            int counter = 1;
            for (Iterator i=parameters.iterator(); i.hasNext(); counter++) {
                Object v = i.next();
                ps.setObject(counter, v);
            }

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displayLine("Parameters: id = "+entryId, 80));
                log.debug(Formatter.displaySeparator(80));
            }

            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    public void deleteField(
            SourceMapping sourceMapping,
            FieldConfig fieldConfig,
            int entryId) throws Exception {

        String tableName = "penrose_"+mappingId+"_field_"+sourceMapping.getName()+"_"+fieldConfig.getName();

        Collection parameters = new ArrayList();

        StringBuffer sb = new StringBuffer();
        sb.append("delete from ");
        sb.append(tableName);

        if (entryId > 0) {
            sb.append(" where id=?");
            parameters.add(new Integer(entryId));
        }

        String sql = sb.toString();

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = getJDBCConnection();

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                Collection lines = Formatter.split(sql, 80);
                for (Iterator i=lines.iterator(); i.hasNext(); ) {
                    String line = (String)i.next();
                    log.debug(Formatter.displayLine(line, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

            ps = con.prepareStatement(sql);

            int counter = 1;
            for (Iterator i=parameters.iterator(); i.hasNext(); counter++) {
                Object v = i.next();
                ps.setObject(counter, v);
            }

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displayLine("Parameters: id = "+entryId, 80));
                log.debug(Formatter.displaySeparator(80));
            }

            ps.execute();

        } catch (Exception e) {
            log.error(e.getMessage());

        } finally {
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

}
