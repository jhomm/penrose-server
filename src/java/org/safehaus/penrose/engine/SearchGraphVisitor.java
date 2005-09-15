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
import org.safehaus.penrose.SearchResults;
import org.safehaus.penrose.connection.Connection;
import org.safehaus.penrose.graph.GraphVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class SearchGraphVisitor extends GraphVisitor {

    Logger log = LoggerFactory.getLogger(getClass());

    private EngineContext engineContext;
    private EntryDefinition entryDefinition;
    private Source primarySource;

    private Stack stack = new Stack();
    private Set keys = new HashSet();

    public SearchGraphVisitor(
            EngineContext engineContext,
            EntryDefinition entryDefinition,
            Collection filters,
            Source primarySource) {

        this.engineContext = engineContext;
        this.entryDefinition = entryDefinition;
        this.primarySource = primarySource;

        stack.push(filters);
    }

    public boolean preVisitNode(Object node, Object parameter) throws Exception {
        Source source = (Source)node;
        Collection filters = (Collection)stack.peek();

        log.debug("Searching "+source.getName()+" for:");
        for (Iterator i=filters.iterator(); i.hasNext(); ) {
            Row row = (Row)i.next();
            log.debug(" - "+row);
        }

        if (entryDefinition.getSource(source.getName()) == null) {
            log.debug("Source "+source.getName()+" is not defined in entry "+entryDefinition.getDn());
            return true;
        }

        Map map = engineContext.getSyncService().search(source, filters);
        if (map.size() == 0) return false;

        Collection results = new ArrayList();
        for (Iterator i=map.values().iterator(); i.hasNext(); ) {
            AttributeValues av = (AttributeValues)i.next();
            Collection list = engineContext.getTransformEngine().convert(av);
            for (Iterator j=list.iterator(); j.hasNext(); ) {
                Row row = (Row)j.next();
                Row newRow = new Row();
                for (Iterator k=row.getNames().iterator(); k.hasNext(); ) {
                    String name = (String)k.next();
                    Object value = row.get(name);
                    if (value == null) continue;
                    newRow.set(source.getName()+"."+name, value);
                }
                results.add(newRow);
            }
        }

        stack.push(results);

        keys.addAll(results);

        if (source != primarySource) {
            log.debug("Source "+source.getName()+" is not the primary source of entry "+entryDefinition.getDn());
            return true;
        }

        return false;
    }

    public void postVisitNode(Object node, Object parameter) throws Exception {
        stack.pop();
    }

    public boolean preVisitEdge(Object node1, Object node2, Object object, Object parameter) throws Exception {
        Source source = (Source)node2;
        Relationship relationship = (Relationship)object;

        log.debug("Relationship "+relationship);
        Collection rows = (Collection)stack.peek();

        String lhs = relationship.getLhs();
        String rhs = relationship.getRhs();

        if (lhs.startsWith(source.getName()+".")) {
            String exp = lhs;
            lhs = rhs;
            rhs = exp;
        }

        Collection newRows = new HashSet();
        for (Iterator i=rows.iterator(); i.hasNext(); ) {
            Row row = (Row)i.next();
            log.debug(" - "+row);

            Object value = row.get(lhs);
            if (value == null) continue;

            Row newRow = new Row();
            newRow.set(rhs, value);
            newRows.add(newRow);

            log.debug("   - "+lhs+" -> "+rhs+" = "+value);
        }

        if (newRows.size() == 0) return false;

        stack.push(newRows);

        return true;
    }

    public void postVisitEdge(Object node1, Object node2, Object edge, Object parameter) throws Exception {
        stack.pop();
    }

    public EntryDefinition getEntryDefinition() {
        return entryDefinition;
    }

    public void setEntryDefinition(EntryDefinition entryDefinition) {
        this.entryDefinition = entryDefinition;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public Set getKeys() {
        return keys;
    }

    public void setKeys(Set keys) {
        this.keys = keys;
    }

    public Source getPrimarySource() {
        return primarySource;
    }

    public void setPrimarySource(Source primarySource) {
        this.primarySource = primarySource;
    }
}
