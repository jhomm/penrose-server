/* Generated By:JavaCC: Do not edit this line. ItemParser.java */
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
package org.safehaus.penrose.filter.item;

import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.filter.SimpleFilter;
import org.safehaus.penrose.filter.SubstringFilter;
import org.safehaus.penrose.filter.PresentFilter;

import java.util.*;

/**
 * LDAP Filter Item Parser.
 *
 * <pre>
 *
 *    See: http://www.faqs.org/rfcs/rfc2254.html
 *
 *         item       = simple / present / substring / extensible
 *         simple     = attr filtertype value
 *         filtertype = equal / approx / greater / less
 *         equal      = "="
 *         approx     = "~="
 *         greater    = ">="
 *         less       = "<="
 *         extensible = attr [":dn"] [":" matchingrule] ":=" value
 *                      / [":dn"] ":" matchingrule ":=" value
 *         present    = attr "=*"
 *         substring  = attr "=" [initial] any [final]
 *         initial    = value
 *         any        = "*" *(value "*")
 *         final      = value
 *         attr       = AttributeDescription from Section 4.1.5 of [1]
 *         matchingrule = MatchingRuleId from Section 4.1.9 of [1]
 *         value      = AttributeValue from Section 4.1.6 of [1]
 *
 *    If a value should contain any of the following characters
 *
 *            Character       ASCII value
 *            ---------------------------
 *            *               0x2a
 *            (               0x28
 *            )               0x29
 *            \               0x5c
 *            NUL             0x00
 *
 *    the character must be encoded as the backslash '\' character (ASCII
 *    0x5c) followed by the two hexadecimal digits representing the ASCII
 *    value of the encoded character. The case of the two hexadecimal
 *    digits is not significant.
 *
 *    Example usage:
 *
 *    Reader in = ...;
 *    ItemParser parser = new ItemParser(in);
 *    try {
 *      Filter filter = parser.parse();
 *    } catch (ParseException ex) {
 *      System.out.println(ex.getMessage());
 *    }
 *
 * </pre>
 */

public class ItemParser implements ItemParserConstants {

  Filter parsedItem;

  public Filter getItem() { return this.parsedItem; }

  public Filter parse() throws ParseException {
    parsedItem = Item();
    return parsedItem;
  }

  final public Filter Item() throws ParseException {
    trace_call("Item");
    try {
        Filter filter;
        Token attr, type, value;
        String valueStr = "";
      attr = jj_consume_token(ATTR);
      type = jj_consume_token(TYPE);
      value = jj_consume_token(VALUE);
        valueStr += value.toString();
        if (!"=".equals(type.toString())) {
            filter = new SimpleFilter(attr.toString(), type.toString(), valueStr);

        } else if ("*".equals(valueStr)) {
                filter = new PresentFilter(attr.toString());

        } else if (valueStr.indexOf('*') < 0) {
            filter = new SimpleFilter(attr.toString(), "=", valueStr);

        } else {
            List values = new ArrayList();
            StringTokenizer st = new StringTokenizer(valueStr, "*", true);
            while (st.hasMoreTokens()) {
                values.add(st.nextToken());
            }
            filter = new SubstringFilter(attr.toString(), values);
        }
          {if (true) return filter;}
    throw new Error("Missing return statement in function");
    } finally {
      trace_return("Item");
    }
  }

  public ItemParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  public Token token, jj_nt;
  private int jj_ntk;
  private int jj_gen;
  final private int[] jj_la1 = new int[0];
  static private int[] jj_la1_0;
  static {
      jj_la1_0();
   }
   private static void jj_la1_0() {
      jj_la1_0 = new int[] {};
   }

  public ItemParser(java.io.InputStream stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new ItemParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
  }

  public void ReInit(java.io.InputStream stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
  }

  public ItemParser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new ItemParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
  }

  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
  }

  public ItemParser(ItemParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
  }

  public void ReInit(ItemParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
  }

  final private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      trace_token(token, "");
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
      trace_token(token, " (in getNextToken)");
    return token;
  }

  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  final private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.Vector jj_expentries = new java.util.Vector();
  private int[] jj_expentry;
  private int jj_kind = -1;

  public ParseException generateParseException() {
    jj_expentries.removeAllElements();
    boolean[] la1tokens = new boolean[12];
    for (int i = 0; i < 12; i++) {
      la1tokens[i] = false;
    }
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 0; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 12; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.addElement(jj_expentry);
      }
    }
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = (int[])jj_expentries.elementAt(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  private int trace_indent = 0;
  private boolean trace_enabled = true;

  final public void enable_tracing() {
    trace_enabled = true;
  }

  final public void disable_tracing() {
    trace_enabled = false;
  }

  final private void trace_call(String s) {
    if (trace_enabled) {
      for (int i = 0; i < trace_indent; i++) { System.out.print(" "); }
      System.out.println("Call:   " + s);
    }
    trace_indent = trace_indent + 2;
  }

  final private void trace_return(String s) {
    trace_indent = trace_indent - 2;
    if (trace_enabled) {
      for (int i = 0; i < trace_indent; i++) { System.out.print(" "); }
      System.out.println("Return: " + s);
    }
  }

  final private void trace_token(Token t, String where) {
    if (trace_enabled) {
      for (int i = 0; i < trace_indent; i++) { System.out.print(" "); }
      System.out.print("Consumed token: <" + tokenImage[t.kind]);
      if (t.kind != 0 && !tokenImage[t.kind].equals("\"" + t.image + "\"")) {
        System.out.print(": \"" + t.image + "\"");
      }
      System.out.println(">" + where);
    }
  }

  final private void trace_scan(Token t1, int t2) {
    if (trace_enabled) {
      for (int i = 0; i < trace_indent; i++) { System.out.print(" "); }
      System.out.print("Visited token: <" + tokenImage[t1.kind]);
      if (t1.kind != 0 && !tokenImage[t1.kind].equals("\"" + t1.image + "\"")) {
        System.out.print(": \"" + t1.image + "\"");
      }
      System.out.println(">; Expected token: <" + tokenImage[t2] + ">");
    }
  }

}