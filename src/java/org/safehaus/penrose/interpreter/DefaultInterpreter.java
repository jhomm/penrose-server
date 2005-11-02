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
package org.safehaus.penrose.interpreter;

import bsh.Interpreter;
import bsh.Parser;
import bsh.ParserConstants;

import java.io.StringReader;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author Endi S. Dewata
 */
public class DefaultInterpreter extends org.safehaus.penrose.interpreter.Interpreter {

    Logger log = Logger.getLogger(getClass());

    public Interpreter interpreter;

    public DefaultInterpreter() {
        interpreter = new Interpreter();
    }

    public Collection parse(String script) throws Exception {
        List tokens = new ArrayList();
        try {
            Parser parser = new Parser(new StringReader(script+";"));
            //log.debug("Parsing: "+script);
            bsh.Token token = parser.getNextToken();
            while (token != null && !"".equals(token.image)) {
                //log.debug(" - ["+token.image+"] ("+token.kind+")");

                if (token.kind == ParserConstants.IDENTIFIER) {
                    tokens.add(new Token(token.image, Token.IDENTIFIER));

                } else if (token.kind == ParserConstants.STRING_LITERAL) {
                    tokens.add(new Token(token.image, Token.STRING_LITERAL));

                } else if (token.kind == ParserConstants.DOT) {
                    tokens.add(new Token(token.image, Token.DOT));

                } else {
                    tokens.add(new Token(token.image, Token.OTHER));
                }

                token = parser.getNextToken();
            }

            tokens.remove(tokens.size()-1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tokens;
    }

    public Collection parseVariables(String script) throws Exception {
        Collection tokens = new ArrayList();
        try {
            Parser parser = new Parser(new StringReader(script+";"));
            //log.debug("Parsing: "+script);
            bsh.Token token = parser.getNextToken();
            while (token != null && !"".equals(token.image)) {
                //log.debug(" - ["+token.image+"] ("+token.kind+")");
                if (token.kind == ParserConstants.IDENTIFIER) {
                    tokens.add(token.image);
                }
                token = parser.getNextToken();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tokens;
    }

    public void set(String name, Object value) throws Exception {
        interpreter.set(name, value);
    }

    public Object get(String name) throws Exception {
        int i = name.indexOf(".");
        if (i >= 0) {
            String object = name.substring(0, i);
            if (interpreter.get(object) == null) return null;
        }
        return interpreter.get(name);
    }

    public Object eval(String script) throws Exception {
        try {
            if (script == null) return null;
            return interpreter.eval(script);

        } catch (Exception e) {
            log.debug("BeanShellException: "+e.getMessage(), e);
            throw e;
            //return null;
        }
    }
}
