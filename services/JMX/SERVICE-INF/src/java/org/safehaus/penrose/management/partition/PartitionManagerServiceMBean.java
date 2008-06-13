package org.safehaus.penrose.management.partition;

import org.safehaus.penrose.partition.PartitionConfig;

import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public interface PartitionManagerServiceMBean {

    public Collection<String> getPartitionNames() throws Exception;

    public void startPartitions() throws Exception;
    public void stopPartitions() throws Exception;

    public PartitionConfig getPartitionConfig(String partitionName) throws Exception;

    public void createPartition(PartitionConfig partitionConfig) throws Exception;
    public void updatePartition(String name, PartitionConfig partitionConfig) throws Exception;
    public void removePartition(String name) throws Exception;
}