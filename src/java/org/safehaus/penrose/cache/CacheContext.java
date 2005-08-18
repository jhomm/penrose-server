/**
 * Copyright (c) 1998-2005, Verge Lab., LLC.
 * All rights reserved.
 */
package org.safehaus.penrose.cache;

import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.engine.TransformEngine;
import org.safehaus.penrose.engine.Engine;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.schema.Schema;

/**
 * @author Endi S. Dewata
 */
public interface CacheContext {

    public Engine getEngine() throws Exception;
    public Interpreter newInterpreter() throws Exception;
    public TransformEngine getTransformEngine() throws Exception;
    public FilterTool getFilterTool() throws Exception;
    public Schema getSchema() throws Exception;
}
