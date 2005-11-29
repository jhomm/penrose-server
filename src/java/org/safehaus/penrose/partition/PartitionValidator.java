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
package org.safehaus.penrose.partition;

import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.connector.AdapterConfig;
import org.safehaus.penrose.connector.ConnectionConfig;
import org.safehaus.penrose.schema.Schema;
import org.safehaus.penrose.schema.ObjectClass;
import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.module.ModuleConfig;
import org.ietf.ldap.LDAPDN;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * @author Endi S. Dewata
 */
public class PartitionValidator {

    Logger log = Logger.getLogger(getClass());

    private PenroseConfig penroseConfig;
    private Schema schema;

    public PartitionValidator() {
    }

    public Collection validate(Partition partition) throws Exception {
        Collection results = new ArrayList();

        results.addAll(validateSources(partition));
        results.addAll(validateEntries(partition));
        results.addAll(validateModules(partition));

        return results;
    }

    public Collection validateSources(Partition partition) throws Exception {
        Collection results = new ArrayList();

        for (Iterator i=partition.getConnectionConfigs().iterator(); i.hasNext(); ) {
            ConnectionConfig connectionConfig = (ConnectionConfig)i.next();
            //log.debug("Validating connection "+connectionConfig.getConnectionName());

            String connectionName = connectionConfig.getConnectionName();
            if (connectionName == null || "".equals(connectionName)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing connection name.", null, connectionConfig));
                continue;
            }

            String adapterName = connectionConfig.getConnectionType();
            if (adapterName == null || "".equals(adapterName)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing adapter name.", connectionName, connectionConfig));

            } else if (penroseConfig != null) {
                AdapterConfig adapterConfig = penroseConfig.getAdapterConfig(adapterName);
                if (adapterConfig == null) {
                    results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Invalid adapter name: "+adapterName, connectionName, connectionConfig));
                }
            }

            for (Iterator j=connectionConfig.getSourceDefinitions().iterator(); j.hasNext(); ) {
                SourceDefinition sourceDefinition = (SourceDefinition)j.next();
                //log.debug("Validating source "+connectionConfig.getConnectionName()+"/"+sourceDefinition.getName());

                String sourceName = sourceDefinition.getName();
                if (sourceName == null || "".equals(sourceName)) {
                    results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing source name.", connectionName, connectionConfig));
                    continue;
                }

                String conName = sourceDefinition.getConnectionName();
                if (conName == null || "".equals(conName)) {
                    results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing connection name.", connectionName+"/"+sourceName, sourceDefinition));

                } else if (!conName.equals(connectionName)) {
                    results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Invalid connection name: "+conName, connectionName+"/"+sourceName, sourceDefinition));
                }

                boolean hasPrimaryKey = false;

                for (Iterator k=sourceDefinition.getFieldDefinitions().iterator(); k.hasNext(); ) {
                    FieldDefinition fieldDefinition = (FieldDefinition)k.next();
                    //log.debug("Validating field "+connectionConfig.getConnectionName()+"/"+sourceDefinition.getName()+"."+fieldDefinition.getName());

                    String fieldName = fieldDefinition.getName();
                    if (fieldName == null || "".equals(fieldName)) {
                        results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing field name.", connectionName+"/"+sourceName, sourceDefinition));
                    }

                    hasPrimaryKey |= fieldDefinition.isPrimaryKey();
                }

                if (!hasPrimaryKey) {
                    results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing primary key.", connectionName+"/"+sourceName, sourceDefinition));
                }
            }
        }

        return results;
    }

    public Collection validateEntries(Partition partition) throws Exception {
        Collection results = new ArrayList();

        for (Iterator i=partition.getEntryMappings().iterator(); i.hasNext(); ) {
            EntryMapping entryMapping = (EntryMapping)i.next();
            //log.debug("Validating entry "+entryMapping;

            String rdn = entryMapping.getRdn();
            if (rdn == null || "".equals(rdn)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing RDN.", entryMapping.getDn(), entryMapping));

            } else if (!LDAPDN.isValid(rdn)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Invalid RDN: "+rdn, entryMapping.getDn(), entryMapping));
            }

            String parentDn = entryMapping.getParentDn();
            if (parentDn != null && !LDAPDN.isValid(parentDn)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Invalid parent DN: "+parentDn, entryMapping.getDn(), entryMapping));
            }

            results.addAll(validateEntryObjectClasses(partition, entryMapping));
            results.addAll(validateEntryAttributeTypes(partition, entryMapping));
            results.addAll(validateEntrySources(partition, entryMapping));
        }

        return results;
    }

    public Collection validateEntryObjectClasses(Partition partition, EntryMapping entryMapping) {
        Collection results = new ArrayList();

        if (schema == null) return results;

        //log.debug("Validating entry "+entryMapping"'s object classes");

        Collection missingObjectClasses = new TreeSet();

        Collection objectClasses = entryMapping.getObjectClasses();
        //System.out.println("Checking "+entryMapping" object classes "+objectClasses);

        for (Iterator i=objectClasses.iterator(); i.hasNext(); ) {
            String ocName = (String)i.next();

            ObjectClass objectClass = schema.getObjectClass(ocName);
            if (objectClass == null) {
                results.add(new PartitionValidationResult(PartitionValidationResult.WARNING, "Object class "+ocName+" is not defined in the schema.", entryMapping.getDn(), entryMapping));
            }

            Collection scNames = schema.getAllObjectClassNames(ocName);
            for (Iterator j=scNames.iterator(); j.hasNext(); ) {
                String scName = (String)j.next();
                if ("top".equals(scName)) continue;
                
                if (!objectClasses.contains(scName)) {
                    //System.out.println(" - ["+scName+"] not found in "+objectClasses);
                    missingObjectClasses.add(scName);
                }
            }
        }

        for (Iterator i=missingObjectClasses.iterator(); i.hasNext(); ) {
            String scName = (String)i.next();
            results.add(new PartitionValidationResult(PartitionValidationResult.WARNING, "Missing object class "+scName+".", entryMapping.getDn(), entryMapping));
        }

        return results;
    }

    public Collection validateEntryAttributeTypes(Partition partition, EntryMapping entryMapping) {
        Collection results = new ArrayList();

        if (schema == null) return results;

        //log.debug("Validating entry "+entryMapping"'s attributes");

        for (Iterator i=entryMapping.getAttributeMappings().iterator(); i.hasNext(); ) {
            AttributeMapping attributeMapping = (AttributeMapping)i.next();

            String name = attributeMapping.getName();
            if (name == null || "".equals(name)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing attribute name.", entryMapping.getDn(), entryMapping));
                continue;
            }
/*
            AttributeType attributeType = schema.getAttributeType(name);
            if (attributeType == null) {
                results.add(new ConfigValidationResult(ConfigValidationResult.WARNING, "Attribute type "+name+" is not defined in the schema.", entryMapping       Expression expression = attributeMapping.getExpression();
            if (expression != null) {
                String foreach = expression.getForeach();
                String var = expression.getVar();
                if (foreach != null && (var == null || "".equals(var))) {
                    results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing variable name.", entryMapping.getDn()+"/"+name, entryMapping));
                }
            }
*/
        }

        Collection objectClasses = schema.getObjectClasses(entryMapping);
        for (Iterator i=objectClasses.iterator(); i.hasNext(); ) {
            ObjectClass objectClass = (ObjectClass)i.next();

            Collection requiredAttributes = objectClass.getRequiredAttributes();
            for (Iterator j=requiredAttributes.iterator(); j.hasNext(); ) {
                String atName = (String)j.next();

                AttributeMapping attributeMapping = entryMapping.getAttributeMapping(atName);
                if (attributeMapping == null) {
                    results.add(new PartitionValidationResult(PartitionValidationResult.WARNING, "Attribute "+atName+" is required by "+objectClass.getName()+" object class.", entryMapping.getDn(), entryMapping));
                }
            }
        }
        return results;
    }

    public Collection validateEntrySources(Partition partition, EntryMapping entryMapping) {
        Collection results = new ArrayList();

        for (Iterator i=entryMapping.getSourceMappings().iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();
            //log.debug("Validating entry "+entryMapping"'s sourceMapping "+sourceMapping.getName());

            String alias = sourceMapping.getName();
            if (alias == null || "".equals(alias)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing source alias.", entryMapping.getDn(), entryMapping));
                continue;
            }

            String sourceName = sourceMapping.getSourceName();
            if (sourceName == null || "".equals(sourceName)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing source name.", entryMapping.getDn()+"/"+alias, entryMapping));
            }

            String connectionName = sourceMapping.getConnectionName();
            if (connectionName == null || "".equals(connectionName)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing connection name.", entryMapping.getDn()+"/"+alias, entryMapping));

            } else {
                ConnectionConfig connectionConfig = partition.getConnectionConfig(connectionName);
                if (connectionConfig == null) {
                    results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Invalid connection name: "+connectionConfig, entryMapping.getDn()+"/"+alias, entryMapping));

                } else if (sourceName != null && !"".equals(sourceName)) {
                    SourceDefinition sourceDefinition = connectionConfig.getSourceDefinition(sourceName);

                    if (sourceDefinition == null) {
                        results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Invalid source name: "+sourceName, entryMapping.getDn()+"/"+alias, entryMapping));

                    } else {
                        for (Iterator j=sourceMapping.getFieldMappings().iterator(); j.hasNext(); ) {
                            FieldMapping fieldMapping = (FieldMapping)j.next();
                            //log.debug("Validating entry "+entryMapping"'s fieldMapping "+source.getName()+"."+fieldMapping.getName());

                            String fieldName = fieldMapping.getName();
                            if (fieldName == null || "".equals(fieldName)) {
                                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing field name.", entryMapping.getDn()+"/"+alias, entryMapping));
                                continue;
                            }

                            Expression expression = fieldMapping.getExpression();
                            if (expression != null) {
                                String foreach = expression.getForeach();
                                String var = expression.getVar();
                                if (foreach != null && (var == null || "".equals(var))) {
                                    results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing variable name.", entryMapping.getDn()+"/"+alias+"."+fieldName, entryMapping));
                                }
                            }

                            FieldDefinition fieldDefinition = sourceDefinition.getFieldDefinition(fieldName);
                            if (fieldDefinition == null) {
                                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Invalid field name: "+fieldName, entryMapping.getDn()+"/"+alias+"."+fieldName, entryMapping));
                            }
                        }
                    }
                }
            }
        }

        return results;
    }

    public Collection validateModules(Partition partition) throws Exception {
        Collection results = new ArrayList();

        for (Iterator i=partition.getModuleConfigs().iterator(); i.hasNext(); ) {
            ModuleConfig moduleConfig = (ModuleConfig)i.next();
            //log.debug("Validating module "+moduleConfig.getModuleName());

            String moduleName = moduleConfig.getModuleName();
            if (moduleName == null || "".equals(moduleName)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing module name.", null, moduleConfig));
            }

            String moduleClass = moduleConfig.getModuleClass();
            if (moduleClass == null || "".equals(moduleClass)) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Missing module class name.", moduleName, moduleConfig));
            }

            try {
                Class.forName(moduleClass);
            } catch (Exception e) {
                results.add(new PartitionValidationResult(PartitionValidationResult.ERROR, "Module class not found: "+moduleClass, moduleName, moduleConfig));
            }
        }

        return results;
    }

    public PenroseConfig getServerConfig() {
        return penroseConfig;
    }

    public void setServerConfig(PenroseConfig penroseConfig) {
        this.penroseConfig = penroseConfig;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }
}