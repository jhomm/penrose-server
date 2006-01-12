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
import org.safehaus.penrose.partition.FieldConfig;
import org.safehaus.penrose.partition.SourceConfig;
import org.safehaus.penrose.filter.Filter;

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
public class PersistentEntryCacheStorage extends EntryCacheStorage {

    int mappingId;

    String jdbcConnectionName;
    String jndiConnectionName;

    public void init() throws Exception {
        super.init();

        jdbcConnectionName = getParameter("jdbcConnection");
        jndiConnectionName = getParameter("jndiConnection");

        mappingId = getMappingId();
    }

    public Connection getJDBCConnection() throws Exception {
        return (Connection)getConnectionManager().openConnection(jdbcConnectionName);
    }

    public DirContext getJNDIConnection() throws Exception {
        return (DirContext)getConnectionManager().openConnection(jndiConnectionName);
    }

    public void globalCreate() throws Exception {
        createMappingsTable();
    }

    public void create() throws Exception {

        addMapping();
        mappingId = getMappingId();

        String dn = getEntryMapping().getDn();
        log.debug("Creating cache tables for mapping "+dn+" ("+mappingId+")");

        createEntriesTable();

        Collection attributeMappings = getEntryMapping().getAttributeMappings();
        for (Iterator i=attributeMappings.iterator(); i.hasNext(); ) {
            AttributeMapping attributeMapping = (AttributeMapping)i.next();
            createAttributeTable(attributeMapping);
        }

        Collection sources = getPartition().getEffectiveSourceMappings(getEntryMapping());

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();
            SourceConfig sourceConfig = getPartition().getSourceConfig(sourceMapping.getSourceName());

            Collection fields = sourceConfig.getFieldConfigs();
            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                FieldConfig fieldConfig = (FieldConfig)j.next();
                createFieldTable(sourceMapping, fieldConfig);
            }
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
        String dn = getEntryMapping().getDn();

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

    public void addMapping() throws Exception {
        String dn = getEntryMapping().getDn();

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

    public void dropMappingsTable() throws Exception {
        String sql = "drop table penrose_mappings";

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

    public int getEntryId(String dn) throws Exception {

        String tableName = "penrose_"+mappingId+"_entries";

        StringBuffer sb = new StringBuffer();
        Collection parameters = new ArrayList();

        sb.append("select id from ");
        sb.append(tableName);
        sb.append(" where rdn=? and parentDn=?");

        Row rdn = Entry.getRdn(dn);
        parameters.add(rdn.toString());
        parameters.add(getParentDn());

        String sql = sb.toString();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        int id = 0;

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
                Object value = i.next();
                ps.setObject(counter, value);
            }

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displayLine("Parameters:", 80));
                counter = 1;
                for (Iterator i=parameters.iterator(); i.hasNext(); counter++) {
                    Object value = i.next();
                    log.debug(Formatter.displayLine(" - "+counter+" = "+value, 80));
                }
                log.debug(Formatter.displaySeparator(80));
            }

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

        String tableName = "penrose_"+mappingId+"_entries";

        StringBuffer sb = new StringBuffer();
        Collection parameters = new ArrayList();

        sb.append("insert into ");
        sb.append(tableName);
        sb.append(" values (null, ?, ?)");

        Row rdn = Entry.getRdn(dn);
        parameters.add(rdn.toString());
        parameters.add(getParentDn());

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
                Object value = i.next();
                ps.setObject(counter, value);
            }

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displayLine("Parameters:", 80));
                counter = 1;
                for (Iterator i=parameters.iterator(); i.hasNext(); counter++) {
                    Object value = i.next();
                    log.debug(Formatter.displayLine(" - "+counter+" = "+value, 80));
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

    public void removeEntry(int entryId) throws Exception {

        String tableName = "penrose_"+mappingId+"_entries";

        StringBuffer sb = new StringBuffer();
        Collection parameters = new ArrayList();

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

            if (log.isDebugEnabled() && parameters.size() > 0) {
                log.debug(Formatter.displayLine("Parameter: id = "+entryId, 80));
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

    public void createEntriesTable() throws Exception {

        String tableName = "penrose_"+mappingId+"_entries";

        StringBuffer sb = new StringBuffer();
        sb.append("create table ");
        sb.append(tableName);
        sb.append(" (id integer auto_increment, rdn varchar(255), parentDn varchar(255), primary key (id))");

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

    public void drop() throws Exception {
        if (!getPartition().isDynamic(getEntryMapping())) {
            String dn = getEntryMapping().getDn();
            Row rdn = Entry.getRdn(dn);
            remove(rdn);
        }

        Collection sources = getPartition().getEffectiveSourceMappings(getEntryMapping());

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();
            dropEntrySourceTable(sourceMapping);
        }

        Collection attributeDefinitions = getEntryMapping().getAttributeMappings();
        for (Iterator i=attributeDefinitions.iterator(); i.hasNext(); ) {
            AttributeMapping attributeMapping = (AttributeMapping)i.next();
            dropAttributeTable(attributeMapping);
        }

        dropEntriesTable();
    }

    public void globalDrop() throws Exception {
        dropMappingsTable();
    }

    public void dropEntrySourceTable(SourceMapping sourceMapping) throws Exception {

        SourceConfig sourceConfig = getPartition().getSourceConfig(sourceMapping.getSourceName());

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

    public Entry get(Object pk) throws Exception {
        Row rdn = (Row)pk;
        String dn = rdn+","+getParentDn();
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

            Attributes attributes = sr.getAttributes();
            AttributeValues attributeValues = new AttributeValues();
            for (NamingEnumeration ne2 = attributes.getAll(); ne2.hasMore(); ) {
                Attribute attribute = (Attribute)ne2.next();
                String name = attribute.getID();

                for (NamingEnumeration ne3 = attribute.getAll(); ne3.hasMore(); ) {
                    Object value = ne3.next();
                    attributeValues.add(name, value);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug(Formatter.displaySeparator(80));
                log.debug(Formatter.displayLine("dn: "+dn, 80));

                for (Iterator i=attributeValues.getNames().iterator(); i.hasNext(); ) {
                    String name = (String)i.next();
                    Collection values = attributeValues.get(name);

                    for (Iterator j=values.iterator(); j.hasNext(); ) {
                        Object value = j.next();
                        log.debug(Formatter.displayLine(name+": "+value, 80));
                    }
                }

                log.debug(Formatter.displaySeparator(80));
            }

            entry = new Entry(dn, getEntryMapping(), attributeValues);

            int entryId = getEntryId(dn);

            AttributeValues sourceValues = entry.getSourceValues();
            Collection sources = getPartition().getEffectiveSourceMappings(getEntryMapping());

            for (Iterator i=sources.iterator(); i.hasNext(); ) {
                SourceMapping sourceMapping = (SourceMapping)i.next();

                SourceConfig sourceConfig = getPartition().getSourceConfig(sourceMapping.getSourceName());

                Collection fields = sourceConfig.getFieldConfigs();
                for (Iterator j=fields.iterator(); j.hasNext(); ) {
                    FieldConfig fieldConfig = (FieldConfig)j.next();

                    Collection values = getField(sourceMapping, fieldConfig, entryId);
                    //log.debug(" - "+sourceMapping.getName()+"."+fieldConfig.getName()+": "+values);

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

            ctx = getJNDIConnection();

            if (getEntryMapping().isRdnDynamic()) {

                String baseDn = getParentDn();
                log.debug("Searching persistent cache "+getEntryMapping().getRdn()+","+baseDn+" with filter "+filter);

                SearchControls sc = new SearchControls();
                sc.setReturningAttributes(new String[] { "dn" });
                sc.setSearchScope(SearchControls.ONELEVEL_SCOPE);

                String ldapFilter = filter == null ? "(objectClass=*)" : filter.toString();

                NamingEnumeration ne = ctx.search(baseDn, ldapFilter, sc);

                while (ne.hasMore()) {
                    SearchResult sr = (SearchResult)ne.next();
                    String dn = sr.getName()+","+getParentDn();

                    log.debug(" - "+dn);
                    results.add(dn);
                }

            } else {
                String dn = getEntryMapping().getRdn()+","+(parentDn == null ? getEntryMapping().getParentDn() : parentDn);

                log.debug("Searching persistent cache "+dn);

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
            log.debug("Creating LDAP entry "+dn);
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

        Collection attributeDefinitions = getEntryMapping().getAttributeMappings();
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
        Collection sources = getPartition().getEffectiveSourceMappings(getEntryMapping());

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();

            SourceConfig sourceConfig = getPartition().getSourceConfig(sourceMapping.getSourceName());

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
        if (rdn == null && getParentDn() != null) {
            dn = getParentDn();
        } else {
            dn = getParentDn() == null ? getEntryMapping().getDn() : rdn+","+getParentDn();
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
                //if (rdn == null && "".equals(name)) continue;
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

        Collection sources = getPartition().getEffectiveSourceMappings(getEntryMapping());

        int entryId = getEntryId(dn);

        Collection attributeDefinitions = getEntryMapping().getAttributeMappings();
        for (Iterator i=attributeDefinitions.iterator(); i.hasNext(); ) {
            AttributeMapping attributeDefinition = (AttributeMapping)i.next();

            deleteAttribute(attributeDefinition, entryId);
        }

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();

            SourceConfig sourceConfig = getPartition().getSourceConfig(sourceMapping.getSourceName());

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
