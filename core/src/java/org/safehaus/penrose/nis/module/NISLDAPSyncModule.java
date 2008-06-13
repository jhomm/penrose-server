package org.safehaus.penrose.nis.module;

import org.safehaus.penrose.ldap.*;
import org.safehaus.penrose.module.Module;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.session.Session;
import org.safehaus.penrose.source.Source;
import org.safehaus.penrose.source.SourceManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Endi Sukma Dewata
 */
public class NISLDAPSyncModule extends Module implements NISLDAPSyncModuleMBean {

    String sourcePartitionName;

    Source changes;
    Source errors;

    public void init() throws Exception {

        SourceManager sourceManager = partition.getSourceManager();

        String sourcePartitionName = getParameter("source");
        log.debug("Source: "+sourcePartitionName);
        this.sourcePartitionName = sourcePartitionName;

        String changesName = getParameter("changes");
        log.debug("Errors: "+changesName);
        changes = sourceManager.getSource(changesName);

        String errorsName = getParameter("errors");
        log.debug("Errors: "+errorsName);
        errors = sourceManager.getSource(errorsName);
    }

    public boolean execute(Session session, AddRequest request) throws Exception {

        try {
            AddResponse response = new AddResponse();
            partition.add(session, request, response);

            if (changes != null) {
                Attributes attrs = new Attributes();
                attrs.setValue("time", new Timestamp(System.currentTimeMillis()));
                attrs.setValue("title", "Added "+request.getDn());

                StringBuilder sb = new StringBuilder();
                sb.append("The following operation succeeded:\n\n");

                sb.append(request);
                sb.append("\n");

                attrs.setValue("description", sb.toString());

                changes.add(session, new DN(), attrs);
            }

            return true;

        } catch (Throwable e) {

            if (errors == null) {
                throw new Exception(e);

            } else {

                Attributes attrs = new Attributes();
                attrs.setValue("time", new Timestamp(System.currentTimeMillis()));
                attrs.setValue("title", "Error adding "+request.getDn());

                StringBuilder sb = new StringBuilder();
                sb.append("The following operation failed:\n\n");

                sb.append(request);
                sb.append("\n");

                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw, true));

                sb.append("Exception:\n");
                sb.append(sw);

                attrs.setValue("description", sb.toString());

                errors.add(session, new DN(), attrs);

                return false;
            }
        }
    }

    public boolean execute(Session session, DeleteRequest request) throws Exception {
        try {
            DeleteResponse response = new DeleteResponse();
            partition.delete(session, request, response);

            if (changes != null) {
                Attributes attrs = new Attributes();
                attrs.setValue("time", new Timestamp(System.currentTimeMillis()));
                attrs.setValue("title", "Deleted "+request.getDn());

                StringBuilder sb = new StringBuilder();
                sb.append("The following operation succeeded:\n\n");

                sb.append(request);
                sb.append("\n");

                attrs.setValue("description", sb.toString());

                changes.add(session, new DN(), attrs);
            }

            return true;

        } catch (Throwable e) {

            if (errors == null) {
                throw new Exception(e);

            } else {

                Attributes attrs = new Attributes();
                attrs.setValue("time", new Timestamp(System.currentTimeMillis()));
                attrs.setValue("title", "Error deleting "+request.getDn());

                StringBuilder sb = new StringBuilder();
                sb.append("The following operation failed:\n\n");

                sb.append(request);
                sb.append("\n");

                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw, true));

                sb.append("Exception:\n");
                sb.append(sw);

                attrs.setValue("description", sb.toString());

                errors.add(session, new DN(), attrs);

                return false;
            }
        }
    }

    public boolean execute(Session session, ModifyRequest request) throws Exception {

        try {
            ModifyResponse response = new ModifyResponse();
            partition.modify(session, request, response);

            if (changes != null) {
                Attributes attrs = new Attributes();
                attrs.setValue("time", new Timestamp(System.currentTimeMillis()));
                attrs.setValue("title", "Modified "+request.getDn());

                StringBuilder sb = new StringBuilder();
                sb.append("The following operation succeeded:\n\n");

                sb.append(request);
                sb.append("\n");

                attrs.setValue("description", sb.toString());

                changes.add(session, new DN(), attrs);
            }

            return true;

        } catch (Throwable e) {

            if (errors == null) {
                throw new Exception(e);

            } else {

                Attributes attrs = new Attributes();
                attrs.setValue("time", new Timestamp(System.currentTimeMillis()));
                attrs.setValue("title", "Error modifying "+request.getDn());

                StringBuilder sb = new StringBuilder();
                sb.append("The following operation failed:\n\n");

                sb.append(request);
                sb.append("\n");

                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw, true));

                sb.append("Exception:\n");
                sb.append(sw);

                attrs.setValue("description", sb.toString());

                errors.add(session, new DN(), attrs);

                return false;
            }
        }
    }

    public boolean deleteSubtree(Session session, String baseDn) throws Exception {
        return deleteSubtree(session, new DN(baseDn));
    }

    public boolean deleteSubtree(Session session, final DN baseDn) throws Exception {

        final ArrayList<DN> dns = new ArrayList<DN>();

        SearchRequest request = new SearchRequest();
        request.setDn(baseDn);

        SearchResponse response = new SearchResponse() {
            public void add(SearchResult result) throws Exception {
                DN dn = result.getDn();
                log.debug("Found "+dn);
                if (dn.equals(baseDn)) return;
                dns.add(dn);
            }
            public void close() throws Exception {
                super.close();
                long count = dns.size();
                log.debug("Found "+count+" entries.");
            }
        };

        partition.search(session, request, response);

        log.debug("Waiting for operation to complete.");
        int rc = response.waitFor();
        log.debug("RC: "+rc);

        boolean b = true;
        for (int i=dns.size()-1; i>=0; i--) {
            DN dn = dns.get(i);

            //partition.delete(dn);

            DeleteRequest deleteRequest = new DeleteRequest();
            deleteRequest.setDn(dn);
            if (!execute(session, deleteRequest)) b = false;
        }

        return b;
    }

    public List<DN> getDns(Session session, String baseDn) throws Exception {
        return getDns(session, new DN(baseDn));
    }

    public List<DN> getDns(Session session, final DN baseDn) throws Exception {

        //if (warn) log.warn("Getting DNs in subtree "+baseDn+".");

        final ArrayList<DN> dns = new ArrayList<DN>();

        SearchRequest request = new SearchRequest();
        request.setDn(baseDn);

        SearchResponse response = new SearchResponse() {
            public void add(SearchResult result) throws Exception {
                DN dn = result.getDn();
                Attributes attributes = result.getAttributes();

                //log.debug("Found "+dn);

                Attribute objectClass = attributes.get("objectClass");
                if (objectClass != null && objectClass.containsValue("nisNoSync")) {
                    if (warn) log.warn("No sync "+dn+".");
                    return;
                }

                dns.add(dn);

                totalCount++;

                //if (warn) {
                //    if (totalCount % 100 == 0) log.warn("Found "+totalCount+" entries.");
                //}
            }
        };

        partition.search(session, request, response);

        //log.debug("Waiting for operation to complete.");
        int rc = response.waitFor();
        //log.debug("RC: "+rc);

        //if (warn) log.warn("Found "+response.getTotalCount()+" entries.");

        return dns;
    }

    public void createBase() throws Exception {

        Session adminSession = createAdminSession();

        try {
            Partition sourcePartition = partition.getPartitionContext().getPartition(sourcePartitionName);

            DN sourceSuffix = sourcePartition.getDirectory().getSuffix();
            DN targetSuffix = partition.getDirectory().getSuffix();

            log.debug("##################################################################################################");
            log.debug("Creating "+targetSuffix);

            SearchResult result = sourcePartition.find(adminSession, sourceSuffix);
            Attributes attributes = result.getAttributes();

            AddRequest request = new AddRequest();
            request.setDn(targetSuffix);
            request.setAttributes(attributes);

            AddResponse response = new AddResponse();

            partition.add(adminSession, request, response);

        } finally {
            adminSession.close();
        }
    }

    public void removeBase() throws Exception {

        Session adminSession = createAdminSession();

        try {
            DN targetSuffix = partition.getDirectory().getSuffix();

            log.debug("##################################################################################################");
            log.debug("Creating "+targetSuffix);

            DeleteRequest request = new DeleteRequest();
            request.setDn(targetSuffix);

            DeleteResponse response = new DeleteResponse();

            partition.delete(adminSession, request, response);

        } finally {
            adminSession.close();
        }
    }

    public void create(String targetDn) throws Exception {
        create(new DN(targetDn));
    }

    public void create(DN targetDn) throws Exception {

        Session adminSession = createAdminSession();

        try {
            Partition sourcePartition = partition.getPartitionContext().getPartition(sourcePartitionName);

            DN sourceSuffix = sourcePartition.getDirectory().getSuffix();
            DN targetSuffix = partition.getDirectory().getSuffix();

            log.debug("##################################################################################################");
            log.debug("Creating "+targetDn);

            DN sourceDn = targetDn.getPrefix(targetSuffix).append(sourceSuffix);

            SearchRequest request = new SearchRequest();
            request.setDn(sourceDn);

            SearchResponse response = new SearchResponse();
            sourcePartition.search(adminSession, request, response);

            SearchResult result = response.next();
            Attributes attributes = result.getAttributes();

            AddRequest addRequest = new AddRequest();
            addRequest.setDn(targetDn);
            addRequest.setAttributes(attributes);

            AddResponse addResponse = new AddResponse();

            partition.add(adminSession, addRequest, addResponse);

        } finally {
            adminSession.close();
        }
    }

    public void load(String targetDn) throws Exception {
        load(new DN(targetDn));
    }

    public void load(final DN targetDn) throws Exception {

        final Session adminSession = createAdminSession();

        try {
            Partition sourcePartition = getPartition().getPartitionContext().getPartition(sourcePartitionName);

            DN sourceSuffix = sourcePartition.getDirectory().getSuffix();
            DN targetSuffix = partition.getDirectory().getSuffix();

            log.debug("##################################################################################################");
            log.debug("Loading "+targetDn);

            final DN sourceDn = targetDn.getPrefix(targetSuffix).append(sourceSuffix);
            final int nisDnSize = sourceDn.getSize();

            SearchRequest request = new SearchRequest();
            request.setDn(sourceDn);

            SearchResponse response = new SearchResponse() {
                public void add(SearchResult result) throws Exception {

                    DN dn = result.getDn();
                    if (sourceDn.equals(dn)) return;

                    int dnSize = dn.getSize();
                    DN newDn = dn.getPrefix(dnSize - nisDnSize).append(targetDn);

                    Attributes attributes = result.getAttributes();

                    AddRequest request = new AddRequest();
                    request.setDn(newDn);
                    request.setAttributes(attributes);

                    execute(adminSession, request);
                }
            };

            sourcePartition.search(adminSession, request, response);

            log.debug("Waiting for operation to complete.");
            int rc = response.waitFor();
            log.debug("RC: "+rc);

        } finally {
            adminSession.close();
        }
    }

    public void clear(String targetDn) throws Exception {
        clear(new DN(targetDn));
    }

    public void clear(DN targetDn) throws Exception {

        Session adminSession = createAdminSession();

        try {
            log.debug("##################################################################################################");
            log.debug("Clearing "+targetDn);

            deleteSubtree(adminSession, targetDn);

        } finally {
            adminSession.close();
        }
    }

    public void remove(String targetDn) throws Exception {
        remove(new DN(targetDn));
    }

    public void remove(DN targetDn) throws Exception {

        Session adminSession = createAdminSession();

        try {
            log.debug("##################################################################################################");
            log.debug("Removing "+targetDn);

            deleteSubtree(adminSession, targetDn);

            DeleteRequest request = new DeleteRequest();
            request.setDn(targetDn);

            DeleteResponse response = new DeleteResponse();

            partition.delete(adminSession, request, response);

        } finally {
            adminSession.close();
        }
    }

    public long getCount(String targetDn) throws Exception {
        return getCount(new DN(targetDn));
    }

    public long getCount(DN targetDn) throws Exception {
        
        Session adminSession = createAdminSession();

        try {
            SearchRequest request = new SearchRequest();
            request.setDn(targetDn);
            request.setAttributes(new String[] { "dn" });
            request.setTypesOnly(true);

            SearchResponse response = new SearchResponse();

            partition.search(adminSession, request, response);

            log.debug("Waiting for operation to complete.");
            int rc = response.waitFor();
            log.debug("RC: "+rc);

            if (rc != LDAP.SUCCESS) throw response.getException();

            return response.getTotalCount()-1;

        } finally {
            adminSession.close();
        }
    }

    public boolean synchronize() throws Exception {

        Session adminSession = createAdminSession();

        try {
            DN targetSuffix = partition.getDirectory().getSuffix();

            SearchRequest request = new SearchRequest();
            request.setDn(targetSuffix);
            request.setAttributes(new String[] { "dn" });
            request.setScope(SearchRequest.SCOPE_ONE);

            SearchResponse response = new SearchResponse();

            partition.search(adminSession, request, response);

            boolean b = true;
            for (SearchResult result : response.getAll()) {
                if (!synchronize(adminSession, result.getDn())) b = false;
            }

            return b;

        } finally {
            adminSession.close();
        }
    }

    public boolean synchronize(String targetDn) throws Exception {
        return synchronize(new DN(targetDn));
    }

    public boolean synchronize(final DN targetDn) throws Exception {

        Session adminSession = createAdminSession();

        try {
            return synchronize(adminSession, targetDn);

        } finally {
            adminSession.close();
        }
    }

    public boolean synchronize(final Session session, final DN targetDn) throws Exception {

        Partition sourcePartition = partition.getPartitionContext().getPartition(sourcePartitionName);

        final DN sourceSuffix = sourcePartition.getDirectory().getSuffix();
        final DN targetSuffix = partition.getDirectory().getSuffix();

        log.debug("##################################################################################################");
        log.warn("Synchronizing "+targetDn);

        final DN sourceDn = targetDn.getPrefix(targetSuffix).append(sourceSuffix);

        final Collection<String> dns = new LinkedHashSet<String>();

        SearchRequest request1 = new SearchRequest();
        request1.setDn(targetDn);
        request1.setAttributes(new String[] { "dn" });

        if (warn) log.warn("Searching existing entries: "+targetDn);

        SearchResponse response1 = new SearchResponse() {
            public void add(SearchResult result) throws Exception {

                DN dn = result.getDn();
                if (dn.equals(targetDn)) return;

                totalCount++;

                String normalizedDn = dn.getNormalizedDn();
                //if (warn) log.warn(" - "+normalizedDn);
                
                dns.add(normalizedDn);

                if (warn) {
                    if (totalCount % 100 == 0) log.warn("Found "+totalCount+" entries.");
                }
            }
        };

        partition.search(session, request1, response1);

        int rc1 = response1.waitFor();
        if (warn) log.warn("Search completed. RC="+rc1+".");

        if (warn) log.warn("Found "+response1.getTotalCount()+" entries.");

        final long[] counters = new long[] { 0, 0, 0 };
        final boolean[] success = new boolean[] { true };

        SearchRequest request2 = new SearchRequest();
        request2.setDn(sourceDn);

        if (warn) log.warn("Searching new entries: "+sourceDn);

        SearchResponse response2 = new SearchResponse() {
            public void add(SearchResult result2) throws Exception {

                DN dn2 = result2.getDn();
                if (dn2.equals(sourceDn)) return;

                totalCount++;

                DN dn1 = dn2.getPrefix(sourceSuffix).append(targetSuffix);
                String normalizedDn = dn1.getNormalizedDn();

                if (dns.contains(normalizedDn)) {

                    SearchResult result1 = partition.find(session, dn1);

                    Attributes attributes1 = result1.getAttributes();
                    Attribute objectClass = attributes1.get("objectClass");
                    if (objectClass != null && objectClass.containsValue("nisNoSync")) {
                        if (warn) log.warn("No sync "+normalizedDn+".");
                        return;
                    }

                    Attributes attributes2 = result2.getAttributes();

                    convertAutomount(attributes2, sourceSuffix, targetSuffix);

                    Collection<Modification> modifications = LDAP.createModifications(
                            attributes1,
                            attributes2
                    );

                    if (modifications.isEmpty()) {
                        //if (warn) log.warn("No changes, skipping "+normalizedDn+".");

                    } else { // modify entry

                        //if (warn) log.warn("Modifying "+normalizedDn+".");

                        ModifyRequest request = new ModifyRequest();
                        request.setDn(dn1);
                        request.setModifications(modifications);
                        if (!execute(session, request)) success[0] = false;

                        counters[1]++;
                    }

                    dns.remove(normalizedDn);

                } else { // add entry

                    //if (warn) log.warn("Adding "+normalizedDn+".");

                    AddRequest request = new AddRequest();
                    request.setDn(dn1);
                    request.setAttributes(result2.getAttributes());
                    if (!execute(session, request)) success[0] = false;

                    counters[0]++;
                }

                if (warn) {
                    if (totalCount % 100 == 0) log.warn("Processed "+totalCount+" entries.");
                }
            }
        };

        sourcePartition.search(session, request2, response2);

        int rc2 = response2.waitFor();
        if (warn) log.warn("Search completed. RC="+rc2+".");

        for (String normalizedDn : dns) { // deleting entry
            
            List<DN> list = getDns(session, normalizedDn);

            //DeleteRequest request = new DeleteRequest();
            //request.setDn(normalizedDn);
            //if (!execute(request)) success[0] = false;

            for (int i=list.size()-1; i>=0; i--) {
                DN dn = list.get(i);

                //if (warn) log.warn("Deleting "+dn+".");

                DeleteRequest deleteRequest = new DeleteRequest();
                deleteRequest.setDn(dn);
                if (!execute(session, deleteRequest)) success[0] = false;

                counters[2]++;
            }
        }

        if (warn) {
            log.warn("Processed "+response2.getTotalCount()+" entries.");
            log.warn("Added "+counters[0]+" entries.");
            log.warn("Modified "+counters[1]+" entries.");
            log.warn("Deleted "+counters[2]+" entries.");
        }

        return success[0];
    }

    public void convertAutomount(Attributes attributes, DN sourceSuffix, DN targetSuffix) throws Exception {

        Attribute attribute = attributes.get("nisMapEntry");
        if (attribute == null) return;

        Collection<Object> removeList = new ArrayList<Object>();
        Collection<Object> addList = new ArrayList<Object>();

        for (Object object : attribute.getValues()) {
            String value = object.toString();
            if (!value.startsWith("ldap:")) continue;
            int i = value.indexOf(' ', 5);

            String name;
            String info;

            if (i < 0) {
                name = value.substring(5);
                info = null;
            } else {
                name = value.substring(5, i);
                info = value.substring(i+1);
            }

            DN dn = new DN(name);
            DN newDn = dn.getPrefix(sourceSuffix).append(targetSuffix);

            String newValue = "ldap:"+newDn+(info == null ? "" : " "+info);

            removeList.add(value);
            addList.add(newValue);
        }

        attribute.removeValues(removeList);
        attribute.addValues(addList);
    }
/*
    public boolean synchronize(final DN targetDn) throws Exception {

        Partition sourcePartition = partition.getPartitionContext().getPartition(sourcePartitionName);

        final DN sourceSuffix = sourcePartition.getDirectory().getRootEntries().iterator().next().getDn();
        final DN targetSuffix = partition.getDirectory().getRootEntries().iterator().next().getDn();

        log.debug("##################################################################################################");
        log.debug("Synchronizing "+targetDn);

        final DN sourceDn = targetDn.getPrefix(targetSuffix).append(sourceSuffix);

        final Collection<DN> dns1 = new TreeSet<DN>();
        final Map<DN,DN> dns2 = new TreeMap<DN,DN>();

        SearchRequest request1 = new SearchRequest();
        request1.setDn(targetDn);
        request1.setAttributes(new String[] { "dn" });

        SearchResponse response1 = new SearchResponse() {
            public void add(SearchResult result) throws Exception {
                DN dn = result.getDn();
                if (dn.equals(targetDn)) return;
                dns1.add(dn);
            }
        };

        partition.search(request1, response1);

        SearchRequest request2 = new SearchRequest();
        request2.setDn(sourceDn);
        request2.setAttributes(new String[] { "dn" });

        SearchResponse response2 = new SearchResponse() {
            public void add(SearchResult result) throws Exception {
                DN dn = result.getDn();
                if (dn.equals(sourceDn)) return;
                DN newDn = dn.getPrefix(sourceSuffix).append(targetSuffix);
                dns2.put(newDn, dn);
            }
        };

        sourcePartition.search(request2, response2);

        log.debug("Waiting for operation to complete.");
        int rc1 = response1.getReturnCode();
        log.debug("RC: "+rc1);

        log.debug("Waiting for operation to complete.");
        int rc2 = response2.getReturnCode();
        log.debug("RC: "+rc2);

        Iterator<DN> i1 = dns1.iterator();
        Iterator<DN> i2 = dns2.keySet().iterator();

        boolean b1 = i1.hasNext();
        boolean b2 = i2.hasNext();

        DN dn1 = b1 ? i1.next() : null;
        DN dn2 = b2 ? i2.next() : null;

        boolean success = true;

        while (b1 && b2) {

            if (debug) log.debug("Comparing ["+dn1+"] with ["+dn2+"]");

            int c = dn1.compareTo(dn2);

            if (c < 0) { // delete old entry
                DeleteRequest request = new DeleteRequest();
                request.setDn(dn1);
                if (!execute(request)) success = false;

                b1 = i1.hasNext();
                if (b1) dn1 = i1.next();

            } else if (c > 0) { // add new entry

                DN origDn = dns2.get(dn2);
                SearchResult result = sourcePartition.find(origDn);

                AddRequest request = new AddRequest();
                request.setDn(dn2);
                request.setAttributes(result.getAttributes());
                if (!execute(request)) success = false;

                b2 = i2.hasNext();
                if (b2) dn2 = i2.next();

            } else {

                SearchResult result1 = partition.find(dn1);

                DN origDn = dns2.get(dn2);
                SearchResult result2 = sourcePartition.find(origDn);

                Collection<Modification> modifications = createModifications(
                        result1.getAttributes(),
                        result2.getAttributes()
                );

                if (!modifications.isEmpty()) { // modify entry
                    ModifyRequest request = new ModifyRequest();
                    request.setDn(dn1);
                    request.setModifications(modifications);
                    if (!execute(request)) success = false;
                }

                b1 = i1.hasNext();
                if (b1) dn1 = i1.next();

                b2 = i2.hasNext();
                if (b2) dn2 = i2.next();
            }
        }

        while (b1) { // delete old entries
            DeleteRequest request = new DeleteRequest();
            request.setDn(dn1);
            if (!execute(request)) success = false;

            b1 = i1.hasNext();
            if (b1) dn1 = i1.next();
        }

        while (b2) { // add new entries

            DN origDn = dns2.get(dn2);
            SearchResult result = sourcePartition.find(origDn);

            AddRequest request = new AddRequest();
            request.setDn(dn2);
            request.setAttributes(result.getAttributes());
            if (!execute(request)) success = false;

            b2 = i2.hasNext();
            if (b2) dn2 = i2.next();
        }

        return success;
    }
*/
}