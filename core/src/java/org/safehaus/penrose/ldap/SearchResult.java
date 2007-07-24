package org.safehaus.penrose.ldap;

import org.safehaus.penrose.control.Control;
import org.safehaus.penrose.mapping.EntryMapping;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.entry.SourceValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class SearchResult {

    public Logger log = LoggerFactory.getLogger(getClass());

    private EntryMapping entryMapping;

    private DN dn;
    private Attributes attributes;

    private SourceValues sourceValues = new SourceValues();

    private Collection<Control> controls;

    public SearchResult() {
        attributes = new Attributes();
        controls = new ArrayList<Control>();
    }

    /**
     * Create a search result with the given DN and attributes.
     *
     * This object will take the ownership of the parameters.
     *
     * @param dn DN
     * @param attributes Attributes
     */
    public SearchResult(String dn, Attributes attributes) {
        this.dn = new DN(dn);
        this.attributes = attributes;
        controls = new ArrayList<Control>();
    }

    /**
     * Create a search result with the given DN and attributes.
     *
     * This object will take the ownership of the parameters.
     *
     * @param dn DN
     * @param attributes Attributes
     */
    public SearchResult(DN dn, Attributes attributes) {
        this.dn = dn;
        this.attributes = attributes;
        controls = new ArrayList<Control>();
    }

    /**
     * Create a search result with the given DN, attributes, and controls.
     *
     * This object will take the ownership of the parameters.
     *
     * @param dn DN
     * @param attributes Attributes
     * @param controls Controls
     */
    public SearchResult(DN dn, Attributes attributes, Collection<Control> controls) {
        this.dn = dn;
        this.attributes = attributes;
        this.controls = controls;
    }

    public DN getDn() {
        return dn;
    }

    public void setDn(DN dn) {
        this.dn = dn;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    /**
     * Set the attributes of this search result.
     *
     * This object will take the ownership of the parameters.
     *
     * @param attributes Attributes
     */
    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public Collection<Control> getControls() {
        return controls;
    }

    /**
     * Set the controls of this search result.
     *
     * This object will take the ownership of the parameters.
     *
     * @param controls Controls
     */
    public void setControls(Collection<Control> controls) {
        this.controls = controls;
    }

    public EntryMapping getEntryMapping() {
        return entryMapping;
    }

    public void setEntryMapping(EntryMapping entryMapping) {
        this.entryMapping = entryMapping;
    }

    public SourceValues getSourceValues() {
        return sourceValues;
    }
    
    /**
     * Set the source values of this search result.
     *
     * This object will take the ownership of the parameters.
     *
     * @param sourceValues Source values
     */
    public void setSourceValues(SourceValues sourceValues) {
        this.sourceValues = sourceValues;
    }

    public void print() throws Exception {

        log.debug(Formatter.displaySeparator(80));
        log.debug(Formatter.displayLine("Search Result: "+dn, 80));

        for (Attribute attribute : attributes.getAll()) {
            String name = attribute.getName();

            for (Object value : attribute.getValues()) {
                String className = value.getClass().getName();
                className = className.substring(className.lastIndexOf(".") + 1);

                log.debug(Formatter.displayLine(" - " + name + ": " + value + " (" + className + ")", 80));
            }
        }

        for (String sourceName : sourceValues.getNames()) {
            Attributes attrs = sourceValues.get(sourceName);

            for (Attribute attribute : attrs.getAll()) {
                String fieldName = sourceName + "." + attribute.getName();

                for (Object value : attribute.getValues()) {
                    String className = value.getClass().getName();
                    className = className.substring(className.lastIndexOf(".") + 1);

                    log.debug(Formatter.displayLine(" - " + fieldName + ": " + value + " (" + className + ")", 80));
                }
            }
        }

        log.debug(Formatter.displaySeparator(80));
    }
}