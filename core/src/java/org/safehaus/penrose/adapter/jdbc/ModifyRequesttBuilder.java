package org.safehaus.penrose.adapter.jdbc;

import org.safehaus.penrose.session.Modification;
import org.safehaus.penrose.session.ModifyRequest;
import org.safehaus.penrose.session.ModifyResponse;
import org.safehaus.penrose.entry.Attribute;
import org.safehaus.penrose.entry.AttributeValues;
import org.safehaus.penrose.mapping.FieldMapping;
import org.safehaus.penrose.mapping.Expression;
import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.jdbc.DeleteStatement;
import org.safehaus.penrose.jdbc.UpdateStatement;
import org.safehaus.penrose.jdbc.InsertStatement;
import org.safehaus.penrose.jdbc.UpdateRequest;
import org.safehaus.penrose.source.SourceRef;
import org.safehaus.penrose.source.FieldRef;
import org.safehaus.penrose.source.Field;
import org.safehaus.penrose.filter.SimpleFilter;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.filter.Filter;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class ModifyRequesttBuilder {

    Logger log = LoggerFactory.getLogger(getClass());

    Collection sources;

    AttributeValues sourceValues;
    Interpreter interpreter;

    ModifyRequest request;
    ModifyResponse response;

    Collection requests = new ArrayList();

    public ModifyRequesttBuilder(
            Collection sources,
            AttributeValues sourceValues,
            Interpreter interpreter,
            ModifyRequest request,
            ModifyResponse response
    ) throws Exception {

        this.sources = sources;
        this.sourceValues = sourceValues;

        this.interpreter = interpreter;

        this.request = request;
        this.response = response;
    }

    public Collection generate() throws Exception {

        boolean first = true;
        for (Iterator i= sources.iterator(); i.hasNext(); ) {
            SourceRef sourceRef = (SourceRef)i.next();

            if (first) {
                generatePrimaryRequest(sourceRef);
                first = false;

            } else {
                generateSecondaryRequests(sourceRef);
            }
        }

        return requests;
    }

    public void generatePrimaryRequest(
            SourceRef sourceRef
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Processing source "+sourceName);

        UpdateStatement statement = new UpdateStatement();
        Collection parameters = new ArrayList();

        statement.setSource(sourceRef);

        Collection modifications = request.getModifications();
        for (Iterator i=modifications.iterator(); i.hasNext(); ) {
            Modification modification = (Modification)i.next();

            int type = modification.getType();
            Attribute attribute = modification.getAttribute();

            String attributeName = attribute.getName();
            Collection attributeValues = attribute.getValues();

            if (debug) {
                switch (type) {
                    case Modification.ADD:
                        log.debug("Adding attribute "+attributeName+": "+attributeValues);
                        break;
                    case Modification.REPLACE:
                        log.debug("Replacing attribute "+attributeName+": "+attributeValues);
                        break;
                    case Modification.DELETE:
                        log.debug("Deleting attribute "+attributeName+": "+attributeValues);
                        break;
                }
            }

            Object attributeValue = attribute.getValue(); // use only the first value

            interpreter.set(sourceValues);
            interpreter.set(attributeName, attributeValue);

            switch (type) {
                case Modification.ADD:
                case Modification.REPLACE:
                    for (Iterator j= sourceRef.getFieldRefs().iterator(); j.hasNext(); ) {
                        FieldRef fieldRef = (FieldRef)j.next();
                        Field field = fieldRef.getField();

                        FieldMapping fieldMapping = fieldRef.getFieldMapping();

                        Object value = interpreter.eval(fieldMapping);
                        if (value == null) continue;

                        String fieldName = field.getName();
                        if (debug) log.debug("Setting field "+fieldName+" to "+value);

                        statement.addField(fieldRef);
                        parameters.add(new Parameter(field, value));
                    }
                    break;

                case Modification.DELETE:
                    for (Iterator j= sourceRef.getFieldRefs().iterator(); j.hasNext(); ) {
                        FieldRef fieldRef = (FieldRef)j.next();
                        Field field = fieldRef.getField();

                        FieldMapping fieldMapping = fieldRef.getFieldMapping();

                        String variable = fieldMapping.getVariable();
                        if (variable == null) continue;

                        if (!variable.equals(attributeName)) continue;
                        
                        String fieldName = field.getName();
                        if (debug) log.debug("Setting field "+fieldName+" to null");

                        statement.addField(fieldRef);
                        parameters.add(new Parameter(field, null));
                    }
                    break;
            }

            interpreter.clear();
        }

        if (statement.isEmpty()) return;

        Filter filter = null;

        for (Iterator i=sourceValues.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = sourceValues.getOne(name);

            int p = name.indexOf(".");
            String sn = name.substring(0, p);
            String fn = name.substring(p+1);

            if (!sourceName.equals(sn)) continue;

            FieldRef fieldRef = sourceRef.getFieldRef(fn);
            Field field = fieldRef.getField();

            SimpleFilter sf = new SimpleFilter(fn, "=", "?");
            filter = FilterTool.appendAndFilter(filter, sf);

            parameters.add(new Parameter(field, value));
        }

        statement.setFilter(filter);

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);
        updateRequest.setParameters(parameters);

        requests.add(updateRequest);
    }

    public void generateSecondaryRequests(
            SourceRef sourceRef
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Processing source "+sourceName);

        Collection modifications = request.getModifications();
        for (Iterator i=modifications.iterator(); i.hasNext(); ) {
            Modification modification = (Modification)i.next();

            int type = modification.getType();
            Attribute attribute = modification.getAttribute();

            String attributeName = attribute.getName();
            Collection attributeValues = attribute.getValues();

            if (debug) {
                switch (type) {
                    case Modification.ADD:
                        log.debug("Adding attribute "+attributeName+": "+attributeValues);
                        break;
                    case Modification.REPLACE:
                        log.debug("Replacing attribute "+attributeName+": "+attributeValues);
                        break;
                    case Modification.DELETE:
                        log.debug("Deleting attribute "+attributeName+": "+attributeValues);
                        break;
                }
            }

            if (attributeValues.isEmpty()) {
                for (Iterator k= sourceRef.getFieldRefs().iterator(); k.hasNext(); ) {
                    FieldRef fieldRef = (FieldRef)k.next();
                    FieldMapping fieldMapping = fieldRef.getFieldMapping();

                    String variable = fieldMapping.getVariable();
                    if (variable != null) {
                        if (variable.indexOf(".") >= 0) continue; // skip foreign key
                    }

                    if (attributeName.equals(variable)) {
                        generateDeleteStatement(
                                sourceRef
                        );
                        continue;
                    }

                    Expression expression = fieldMapping.getExpression();
                    if (expression == null) continue;

                    String foreach = expression.getForeach();
                    if (foreach == null) continue;

                    if (attributeName.equals(foreach)) {
                        generateDeleteStatement(
                                sourceRef
                        );
                        continue;
                    }

                }
                continue;
            }
            
            boolean first = true;

            for (Iterator j=attributeValues.iterator(); j.hasNext(); ) {
                Object attributeValue = j.next();

                interpreter.set(sourceValues);
                interpreter.set(attributeName, attributeValue);

                Map values = new HashMap();

                for (Iterator k= sourceRef.getFieldRefs().iterator(); k.hasNext(); ) {
                    FieldRef fieldRef = (FieldRef)k.next();
                    FieldMapping fieldMapping = fieldRef.getFieldMapping();

                    String variable = fieldMapping.getVariable();
                    if (variable != null) {
                        if (variable.indexOf(".") >= 0) continue; // skip foreign key
                    }

                    String fieldName = fieldRef.getName();
                    Object value = interpreter.eval(fieldMapping);
                    if (value == null) continue;

                    values.put(fieldName, value);
                }

                if (values.isEmpty()) continue;

                switch (type) {
                    case Modification.ADD:
                        generateInsertStatement(
                                sourceRef,
                                values
                        );
                        break;

                    case Modification.REPLACE:
                        if (first) {
                            generateDeleteStatement(
                                    sourceRef
                            );
                            first = false;
                        }
                        generateInsertStatement(
                                sourceRef,
                                values
                        );
                        break;

                    case Modification.DELETE:
                        generateDeleteStatement(
                                sourceRef,
                                values
                        );
                        break;
                }

                interpreter.clear();
            }

        }
    }

    public void generateInsertStatement(
            SourceRef sourceRef,
            Map values
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Inserting values into "+sourceName);

        InsertStatement statement = new InsertStatement();
        Collection parameters = new ArrayList();

        statement.setSource(sourceRef.getSource());

        for (Iterator k= sourceRef.getFieldRefs().iterator(); k.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)k.next();
            Field field = fieldRef.getField();

            FieldMapping fieldMapping = fieldRef.getFieldMapping();

            String variable = fieldMapping.getVariable();
            if (variable == null) continue;

            Object value = sourceValues.getOne(variable);
            if (value == null) continue;

            String fieldName = field.getName();

            if (debug) log.debug(" - Field: "+fieldName+": "+value);
            statement.addField(fieldRef.getField());
            parameters.add(new Parameter(field, value));
        }

        for (Iterator i=values.keySet().iterator(); i.hasNext(); ) {
            String fieldName = (String)i.next();
            Object value = values.get(fieldName);

            FieldRef fieldRef = sourceRef.getFieldRef(fieldName);
            Field field = fieldRef.getField();

            if (debug) log.debug(" - Field: "+fieldName+": "+value);
            statement.addField(field);
            parameters.add(new Parameter(field, value));
        }

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);
        updateRequest.setParameters(parameters);

        requests.add(updateRequest);
    }

    public void generateDeleteStatement(
            SourceRef sourceRef
    ) throws Exception {
        generateDeleteStatement(sourceRef, null);
    }

    public void generateDeleteStatement(
            SourceRef sourceRef,
            Map values
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Deleting rows from "+sourceName);

        DeleteStatement statement = new DeleteStatement();
        Collection parameters = new ArrayList();

        statement.setSource(sourceRef.getSource());

        Filter filter = null;

        for (Iterator k= sourceRef.getFieldRefs().iterator(); k.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)k.next();
            Field field = fieldRef.getField();

            FieldMapping fieldMapping = fieldRef.getFieldMapping();

            String variable = fieldMapping.getVariable();
            if (variable == null) continue;

            Object value = sourceValues.getOne(variable);
            if (value == null) continue;

            String fieldName = field.getName();

            SimpleFilter sf = new SimpleFilter(fieldName, "=", "?");
            filter = FilterTool.appendAndFilter(filter, sf);

            if (debug) log.debug(" - Field: "+fieldName+": "+value);
            parameters.add(new Parameter(field, value));
        }

        if (values != null) {
            for (Iterator i=values.keySet().iterator(); i.hasNext(); ) {
                String fieldName = (String)i.next();
                Object value = values.get(fieldName);

                FieldRef fieldRef = sourceRef.getFieldRef(fieldName);
                Field field = fieldRef.getField();

                SimpleFilter sf = new SimpleFilter(fieldName, "=", "?");
                filter = FilterTool.appendAndFilter(filter, sf);

                if (debug) log.debug(" - Field: "+fieldName+": "+value);
                parameters.add(new Parameter(field, value));
            }
        }

        statement.setFilter(filter);

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);
        updateRequest.setParameters(parameters);

        requests.add(updateRequest);
    }
}
