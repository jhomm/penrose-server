package org.safehaus.penrose.adapter.jdbc;

import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.mapping.EntryMapping;
import org.safehaus.penrose.mapping.FieldMapping;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.session.SearchRequest;
import org.safehaus.penrose.session.SearchResponse;
import org.safehaus.penrose.entry.AttributeValues;
import org.safehaus.penrose.jdbc.SelectStatement;
import org.safehaus.penrose.jdbc.QueryRequest;
import org.safehaus.penrose.source.SourceRef;
import org.safehaus.penrose.source.FieldRef;
import org.safehaus.penrose.filter.Filter;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class SearchRequestBuilder {

    Logger log = LoggerFactory.getLogger(getClass());

    Map sources = new LinkedHashMap(); // need to maintain order

    AttributeValues sourceValues;
    Interpreter interpreter;

    SearchRequest request;
    SearchResponse response;

    FilterBuilder filterBuilder;

    public SearchRequestBuilder(
            Partition partition,
            EntryMapping entryMapping,
            Collection sources,
            AttributeValues sourceValues,
            Interpreter interpreter,
            SearchRequest request,
            SearchResponse response
    ) throws Exception {
        
        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceRef sourceRef = (SourceRef)i.next();
            this.sources.put(sourceRef.getAlias(), sourceRef);
        }

        this.sourceValues = sourceValues;

        this.interpreter = interpreter;

        this.request = request;
        this.response = response;

        filterBuilder = new FilterBuilder(
                entryMapping,
                sources,
                sourceValues,
                interpreter
        );
    }

    public String generateJoinType(SourceRef sourceRef) {
        if (sourceRef.isRequired()) return "join";
        return "left join";
    }

    public String generateJoinOn(SourceRef sourceRef) {
        return generateJoinOn(sourceRef, sourceRef.getAlias());
    }

    public String generateJoinOn(SourceRef sourceRef, String alias) {

        boolean debug = log.isDebugEnabled();

        StringBuffer sb = new StringBuffer();

        if (debug) log.debug(" - Foreign keys:");
        for (Iterator j= sourceRef.getFieldRefs().iterator(); j.hasNext(); ) {
            FieldRef fieldRef = (FieldRef)j.next();

            FieldMapping fieldMapping = fieldRef.getFieldMapping();

            String variable = fieldMapping.getVariable();
            if (variable == null) continue;

            int p = variable.indexOf(".");
            if (p < 0) continue;

            String lhs = alias+"."+ fieldRef.getOriginalName();

            String sn = variable.substring(0, p);
            String fn = variable.substring(p+1);

            SourceRef s = (SourceRef)sources.get(sn);
            FieldRef f = s.getFieldRef(fn);
            String rhs = sn+"."+f.getOriginalName();

            if (debug) log.debug("   - "+lhs+": "+rhs);

            if (sb.length() > 0) sb.append(" and ");
            sb.append(lhs);
            sb.append("=");
            sb.append(rhs);
        }

        return sb.toString();
    }

    public QueryRequest generate() throws Exception {

        boolean debug = log.isDebugEnabled();

        SelectStatement statement = new SelectStatement();
        Collection parameters = new ArrayList();

        int sourceCounter = 0;
        for (Iterator i= sources.values().iterator(); i.hasNext(); sourceCounter++) {
            SourceRef sourceRef = (SourceRef)i.next();

            String sourceName = sourceRef.getAlias();
            if (debug) log.debug("Processing source "+sourceName);

            for (Iterator j= sourceRef.getFieldRefs().iterator(); j.hasNext(); ) {
                FieldRef fieldRef = (FieldRef)j.next();
                statement.addFieldRef(fieldRef);
            }

            statement.addSourceRef(sourceRef);

            // join previous table
            if (sourceCounter > 0) {
                String joinType = generateJoinType(sourceRef);
                String joinOn = generateJoinOn(sourceRef);

                if (debug) {
                    log.debug(" - Join type: "+joinType);
                    log.debug(" - Join on: "+joinOn);
                }

                statement.addJoin(joinType, joinOn);
            }

            statement.setOrders(sourceRef.getPrimaryKeyFieldRefs());
        }
/*
        for (Iterator i=entryMapping.getRelationships().iterator(); i.hasNext(); ) {
            Relationship relationship = (Relationship)i.next();

            String leftSource = relationship.getLeftSource();
            String rightSource = relationship.getRightSource();

            if (!sourceMappings.containsKey(leftSource) || !sourceMappings.containsKey(rightSource)) continue;
            joinOns.add(relationship.getExpression());
        }
*/
        filterBuilder.append(request.getFilter());

        Map tableAliases = filterBuilder.getSourceAliases();
        for (Iterator i= tableAliases.keySet().iterator(); i.hasNext(); ) {
            String alias = (String)i.next();
            SourceRef sourceRef = (SourceRef)tableAliases.get(alias);

            if (debug) log.debug("Adding source "+alias);
            statement.addSourceRef(alias, sourceRef);

            String joinType = generateJoinType(sourceRef);
            String joinOn = generateJoinOn(sourceRef, alias);

            if (debug) {
                log.debug(" - Join type: "+joinType);
                log.debug(" - Join on: "+joinOn);
            }

            statement.addJoin(joinType, joinOn);
        }

        Filter sourceFilter = filterBuilder.getFilter();
        if (debug) log.debug("Source filter: "+sourceFilter);

        statement.setFilter(sourceFilter);
        parameters.addAll(filterBuilder.getParameters());

/*
        for (Iterator i=sourceMappings.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();
            SourceConfig sourceConfig = partition.getSourceConfig(sourceMapping);

            String defaultFilter = sourceConfig.getParameter(FILTER);

            if (defaultFilter != null) {
                if (debug) log.debug("Default filter: "+defaultFilter);
                filters.add(defaultFilter);
            }
        }
*/

        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setStatement(statement);
        queryRequest.setParameters(parameters);

        return queryRequest;
    }

}
