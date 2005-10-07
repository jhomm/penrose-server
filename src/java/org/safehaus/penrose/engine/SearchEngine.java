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
package org.safehaus.penrose.engine;

import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.config.Config;
import org.safehaus.penrose.graph.Graph;
import org.safehaus.penrose.interpreter.Interpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class SearchEngine {

    Logger log = LoggerFactory.getLogger(getClass());

    private Engine engine;
    private EngineContext engineContext;

    public SearchEngine(Engine engine) {
        this.engine = engine;
        this.engineContext = engine.getEngineContext();
    }

    public Collection search(
            Collection parents,
            EntryDefinition entryDefinition,
            Filter filter)
            throws Exception {

        Entry parent = parents.isEmpty() ? null : (Entry)parents.iterator().next();
        String dn = parent == null ? entryDefinition.getDn() : entryDefinition.getRdn()+","+parent.getDn();
        log.debug("Searching entry "+dn+" for "+filter);

        AttributeValues sourceValues = new AttributeValues();
        //if (parent != null) engine.getFieldValues("parent", dn, sourceValues);

        log.debug("Parent entries:");
        for (Iterator i=parents.iterator(); i.hasNext(); ) {
            Entry entry = (Entry)i.next();
            AttributeValues values = entry.getSourceValues();
            log.debug(" - "+entry.getDn()+": "+values);
            sourceValues.add(values);
        }
        log.debug("Parent values: "+sourceValues);

        Collection newRows = engineContext.getTransformEngine().convert(sourceValues);

        Config config = engineContext.getConfig(entryDefinition.getDn());

        Graph graph = engine.getGraph(entryDefinition);
        Source primarySource = engine.getPrimarySource(entryDefinition);

        Source startingSource;
        Filter newFilter = null;

        Relationship relationship = engine.getConnectingRelationship(entryDefinition);
        if (relationship != null) {
            String lhs = relationship.getLhs();
            String lsourceName = lhs.substring(0, lhs.indexOf("."));
            Source lsource = entryDefinition.getSource(lsourceName);

            String rhs = relationship.getRhs();
            String rsourceName = rhs.substring(0, rhs.indexOf("."));
            Source rsource = entryDefinition.getSource(rsourceName);

            startingSource = lsource == null ? rsource : lsource;

            Collection relationships = new ArrayList();
            relationships.add(relationship);

            newFilter = engine.generateFilter(startingSource, relationships, newRows);

        } else {

            startingSource = primarySource;
            newFilter = engine.getFilterTool().toSourceFilter(sourceValues, entryDefinition, startingSource, filter);
        }

        log.debug("Starting from source: "+(startingSource == null ? null : startingSource.getName()));
        log.debug("With filter: "+newFilter);


        if (startingSource == null) {
            Interpreter interpreter = engineContext.newInterpreter();
            Row rdn = computeRdn(entryDefinition, interpreter);

            Collection rdns = new ArrayList();
            rdns.add(rdn);
            return rdns;

        } else {
            SearchGraphVisitor visitor = new SearchGraphVisitor(config, graph, engine, entryDefinition, sourceValues, newFilter, filter, primarySource);
            graph.traverse(visitor, startingSource);

            Collection rows = new TreeSet();
            rows.addAll(visitor.getKeys());
            return computeRdns(entryDefinition, rows);
        }
    }

    public Collection computeRdns(EntryDefinition entryDefinition, Collection rows) throws Exception {
        Source primarySource = engine.getPrimarySource(entryDefinition);

        log.debug("Search results:");
        Collection rdns = new TreeSet();

        for (Iterator i=rows.iterator(); i.hasNext(); ) {
            Row row = (Row)i.next();
            log.debug(" - "+row);

            Interpreter interpreter = engineContext.newInterpreter();
            for (Iterator k=row.getNames().iterator(); k.hasNext(); ) {
                String name = (String)k.next();
                Object value = row.get(name);
                if (value == null) continue;
                interpreter.set(primarySource.getName()+"."+name, value);
            }

            Row rdn = computeRdn(entryDefinition, interpreter);
            if (rdn == null) continue;

            log.debug("   ==> RDN: "+rdn);
            rdns.add(rdn);
        }

        return rdns;
    }

    public Row computeRdn(EntryDefinition entryDefinition, Interpreter interpreter) throws Exception {
        Row rdn = new Row();

        Collection rdnAttributes = entryDefinition.getRdnAttributes();
        for (Iterator j=rdnAttributes.iterator(); j.hasNext(); ) {
            AttributeDefinition attr = (AttributeDefinition)j.next();
            String name = attr.getName();

            Expression expression = attr.getExpression();
            if (expression == null) continue;

            Object value = interpreter.eval(expression);
            if (value == null) return null;

            rdn.set(name, value);
        }

        Row nrdn = engineContext.getSchema().normalize(rdn);

        return nrdn;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public EngineContext getEngineContext() {
        return engineContext;
    }

    public void setEngineContext(EngineContext engineContext) {
        this.engineContext = engineContext;
    }
}
