package org.safehaus.penrose.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.safehaus.penrose.partition.Partition;

/**
 * @author Endi Sukma Dewata
 */
public class Job {

    public Logger log = LoggerFactory.getLogger(getClass());
    public boolean debug = log.isDebugEnabled();

    protected JobConfig jobConfig;
    protected JobContext jobContext;

    public void init(JobConfig jobConfig, JobContext jobContext) throws Exception {
        this.jobConfig = jobConfig;
        this.jobContext = jobContext;

        log.debug("Initializing "+jobConfig.getName()+" job.");
        
        init();
    }

    public void init() throws Exception {
    }

    public void execute() throws Exception {
    }

    public String getName() {
        return jobConfig.getName();
    }

    public JobConfig getJobConfig() {
        return jobConfig;
    }

    public void setJobConfig(JobConfig jobConfig) {
        this.jobConfig = jobConfig;
    }

    public JobContext getJobContext() {
        return jobContext;
    }

    public void setJobContext(JobContext jobContext) {
        this.jobContext = jobContext;
    }

    public Partition getPartition() {
        return jobContext.getPartition();
    }
}