package org.safehaus.penrose.adapter.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.safehaus.penrose.mapping.FieldMapping;
import org.safehaus.penrose.entry.*;
import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.session.DeleteRequest;
import org.safehaus.penrose.session.DeleteResponse;
import org.safehaus.penrose.source.SourceRef;
import org.safehaus.penrose.source.FieldRef;
import org.safehaus.penrose.source.Source;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Endi S. Dewata
 */
public class DeleteRequestBuilder {

    Logger log = LoggerFactory.getLogger(getClass());

    String suffix;

    Collection sources;
    AttributeValues sourceValues;

    Interpreter interpreter;

    DeleteRequest request;
    DeleteResponse response;

    Collection requests = new ArrayList();

    public DeleteRequestBuilder(
            String suffix,
            Collection sources,
            AttributeValues sourceValues,
            Interpreter interpreter,
            DeleteRequest request,
            DeleteResponse response
    ) throws Exception {

        this.suffix = suffix;

        this.sources = sources;
        this.sourceValues = sourceValues;

        this.interpreter = interpreter;

        this.request = request;
        this.response = response;
    }

    public Collection generate() throws Exception {

        SourceRef sourceRef = (SourceRef) sources.iterator().next();
        generatePrimaryRequest(sourceRef);

        return requests;
    }

    public void generatePrimaryRequest(SourceRef sourceRef) throws Exception {

        boolean debug = log.isDebugEnabled();

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Processing source "+sourceName);

        DeleteRequest newRequest = new DeleteRequest();

        interpreter.set(sourceValues);

        RDN rdn = request.getDn().getRdn();
        for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            Object attributeValue = rdn.get(attributeName);

            interpreter.set(attributeName, attributeValue);
        }

        RDNBuilder rb = new RDNBuilder();

        for (Iterator k= sourceRef.getFieldRefs().iterator(); k.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)k.next();
            FieldMapping fieldMapping = fieldRef.getFieldMapping();

            String fieldName = fieldRef.getName();
            if (!fieldRef.isPrimaryKey()) continue;

            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            if (debug) log.debug(" - Field: "+fieldName+": "+value);
            rb.set(fieldRef.getOriginalName(), value);
        }

        Source source = sourceRef.getSource();
        newRequest.setDn(getDn(source, rb.toRdn()));

        requests.add(newRequest);
    }

    public DN getDn(Source source, RDN rdn) throws Exception {
        String baseDn = source.getParameter(LDAPAdapter.BASE_DN);

        DNBuilder db = new DNBuilder();
        db.append(rdn);
        db.append(baseDn);
        db.append(suffix);
        DN dn = db.toDn();

        return dn;
    }
}
