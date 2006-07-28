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

import org.safehaus.penrose.mapping.EntryMapping;
import org.safehaus.penrose.mapping.Entry;
import org.safehaus.penrose.mapping.Row;
import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.PartitionManager;
import org.safehaus.penrose.partition.SourceConfig;
import org.safehaus.penrose.Penrose;
import org.safehaus.penrose.thread.ThreadManager;
import org.safehaus.penrose.session.PenroseSearchResults;
import org.safehaus.penrose.connector.ConnectionManager;
import org.safehaus.penrose.filter.Filter;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class EntryCache {

    Logger log = LoggerFactory.getLogger(getClass());

    public final static String DEFAULT_CACHE_NAME  = "Entry Cache";
    public final static String DEFAULT_CACHE_CLASS = DefaultEntryCache.class.getName();

    CacheConfig cacheConfig;
    ConnectionManager connectionManager;
    PenroseConfig penroseConfig;
    PartitionManager partitionManager;
    ThreadManager threadManager;

    public Map caches = new TreeMap();
    public Collection listeners = new ArrayList();

    public void addListener(EntryCacheListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EntryCacheListener listener) {
        listeners.remove(listener);
    }

    public Collection getParameterNames() {
        return cacheConfig.getParameterNames();
    }

    public String getParameter(String name) {
        return cacheConfig.getParameter(name);
    }

    public void init() throws Exception {
    }

    public EntryCacheStorage createCacheStorage(EntryMapping entryMapping) throws Exception {

        Partition partition = partitionManager.getPartition(entryMapping);

        EntryCacheStorage cacheStorage = new EntryCacheStorage();
        cacheStorage.setCacheConfig(cacheConfig);
        cacheStorage.setConnectionManager(connectionManager);
        cacheStorage.setPartition(partition);
        cacheStorage.setEntryMapping(entryMapping);

        cacheStorage.init();

        return cacheStorage;
    }

    public EntryCacheStorage getCacheStorage(EntryMapping entryMapping) throws Exception {

        String key = entryMapping.getDn();
        //log.debug("Getting cache storage for "+key);
        //log.debug("entry cache: "+caches.keySet());

        EntryCacheStorage cacheStorage = (EntryCacheStorage)caches.get(key);

        if (cacheStorage == null) {
            //log.debug("Creating new cache storage.");
            cacheStorage = createCacheStorage(entryMapping);
            caches.put(key, cacheStorage);
        }

        return cacheStorage;
    }

    public void add(EntryMapping entryMapping, Filter filter, String dn) throws Exception {
        getCacheStorage(entryMapping).add(filter, dn);
    }
    
    public void search(EntryMapping entryMapping, PenroseSearchResults results) throws Exception {
        getCacheStorage(entryMapping).search(null, (Filter)null, results);
    }

    public Collection search(EntryMapping entryMapping, SourceConfig sourceConfig, Row filter) throws Exception {
        return getCacheStorage(entryMapping).search(sourceConfig, filter);
    }

    public boolean contains(EntryMapping entryMapping, String parentDn, Filter filter) throws Exception {
        return getCacheStorage(entryMapping).contains(parentDn, filter);
    }

    public void search(
            EntryMapping entryMapping,
            String parentDn,
            Filter filter,
            PenroseSearchResults results)
            throws Exception {

        getCacheStorage(entryMapping).search(parentDn, filter, results);
    }

    public void put(Entry entry) throws Exception {
        EntryMapping entryMapping = entry.getEntryMapping();
        getCacheStorage(entryMapping).put(entry.getDn(), entry);

        EntryCacheEvent event = new EntryCacheEvent(entry, EntryCacheEvent.CACHE_ADDED);
        postEvent(event);
    }

    public void put(EntryMapping entryMapping, Filter filter, Collection dns) throws Exception {
        getCacheStorage(entryMapping).put(filter, dns);
    }

    public Entry get(String dn) throws Exception {
        Partition partition = partitionManager.findPartition(dn);
        if (partition == null) throw new Exception("Can't find partition for "+dn);

        EntryMapping entryMapping = partition.findEntryMapping(dn);
        if (entryMapping == null) throw new Exception("Can't entry mapping for "+dn);

        return getCacheStorage(entryMapping).get(dn);
    }

    public void remove(Entry entry) throws Exception {

        EntryMapping entryMapping = entry.getEntryMapping();

        Partition partition = partitionManager.getPartition(entryMapping);
        remove(partition, entryMapping, entry.getDn());
    }

    public void remove(Partition partition, EntryMapping entryMapping, String dn) throws Exception {

        Collection children = partition.getChildren(entryMapping);
        for (Iterator i=children.iterator(); i.hasNext(); ) {
            EntryMapping childMapping = (EntryMapping)i.next();

            PenroseSearchResults childDns = new PenroseSearchResults();
            search(childMapping, dn, null, childDns);

            for (Iterator j=childDns.iterator(); j.hasNext(); ) {
                String childDn = (String)j.next();

                remove(partition, childMapping, childDn);
            }
        }

        log.debug("Remove cache "+dn);

        getCacheStorage(entryMapping).remove(dn);

        EntryCacheEvent event = new EntryCacheEvent(dn, EntryCacheEvent.CACHE_REMOVED);
        postEvent(event);
    }

    public PenroseConfig getPenroseConfig() {
        return penroseConfig;
    }

    public void setPenroseConfig(PenroseConfig penroseConfig) {
        this.penroseConfig = penroseConfig;
    }

    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    public void setPartitionManager(PartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    public void create() throws Exception {
        for (Iterator i=partitionManager.getPartitions().iterator(); i.hasNext(); ) {
            Partition partition = (Partition)i.next();
            Collection entryMappings = partition.getRootEntryMappings();
            create(partition, entryMappings);
        }
    }

    public  void create(Partition partition, Collection entryDefinitions) throws Exception {

        for (Iterator i=entryDefinitions.iterator(); i.hasNext(); ) {
            EntryMapping entryMapping = (EntryMapping)i.next();

            EntryCacheStorage cacheStorage = getCacheStorage(entryMapping);

            log.debug("Creating tables for "+entryMapping.getDn());
            cacheStorage.create();

            Collection children = partition.getChildren(entryMapping);
            create(partition, children);
        }
    }

    public void load(Penrose penrose) throws Exception {
        for (Iterator i=partitionManager.getPartitions().iterator(); i.hasNext(); ) {
            Partition partition = (Partition)i.next();
            load(penrose, partition);
        }
    }

    public void load(Penrose penrose, Partition partition) throws Exception {
    }

    public void clean() throws Exception {
        for (Iterator i=partitionManager.getPartitions().iterator(); i.hasNext(); ) {
            Partition partition = (Partition)i.next();
            Collection entryMappings = partition.getRootEntryMappings();
            clean(partition, entryMappings);
        }
    }

    public void clean(Partition partition, Collection entryDefinitions) throws Exception {

        for (Iterator i=entryDefinitions.iterator(); i.hasNext(); ) {
            EntryMapping entryMapping = (EntryMapping)i.next();

            EntryCacheStorage entryCacheStorage = getCacheStorage(entryMapping);
            if (!entryCacheStorage.contains(null, (Filter)null)) continue;

            PenroseSearchResults dns = new PenroseSearchResults();
            dns = entryCacheStorage.search(null, (Filter)null, dns);

            if (dns == null) continue;

            Collection children = partition.getChildren(entryMapping);
            clean(partition, children);

            for (Iterator j=dns.iterator(); j.hasNext(); ) {
                String dn = (String)j.next();
                remove(partition, entryMapping, dn);
            }
        }
    }

    public void drop() throws Exception {

        for (Iterator i=partitionManager.getPartitions().iterator(); i.hasNext(); ) {
            Partition partition = (Partition)i.next();
            Collection entryMappings = partition.getRootEntryMappings();
            drop(partition, entryMappings);
        }
    }

    public EntryCacheStorage drop(Partition partition, Collection entryDefinitions) throws Exception {

        EntryCacheStorage cacheStorage = null;

        for (Iterator i=entryDefinitions.iterator(); i.hasNext(); ) {
            EntryMapping entryMapping = (EntryMapping)i.next();

            Collection children = partition.getChildren(entryMapping);
            drop(partition, children);

            cacheStorage = getCacheStorage(entryMapping);
            log.debug("Dropping tables for "+entryMapping.getDn());
            cacheStorage.drop();
        }

        return cacheStorage;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

    public void setCacheConfig(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
    }

    public void postEvent(EntryCacheEvent event) throws Exception {

        for (Iterator i=listeners.iterator(); i.hasNext(); ) {
            EntryCacheListener listener = (EntryCacheListener)i.next();

            try {
                switch (event.getType()) {
                    case EntryCacheEvent.CACHE_ADDED:
                        listener.cacheAdded(event);
                        break;
                    case EntryCacheEvent.CACHE_REMOVED:
                        listener.cacheRemoved(event);
                        break;
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public void setThreadManager(ThreadManager threadManager) {
        this.threadManager = threadManager;
    }
}