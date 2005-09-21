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
package org.safehaus.penrose.handler;

import org.safehaus.penrose.PenroseConnection;
import org.safehaus.penrose.event.ModifyEvent;
import org.safehaus.penrose.config.Config;
import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.schema.Schema;
import org.safehaus.penrose.schema.ObjectClass;
import org.safehaus.penrose.util.PasswordUtil;
import org.safehaus.penrose.mapping.*;
import org.ietf.ldap.LDAPDN;
import org.ietf.ldap.LDAPException;
import org.ietf.ldap.LDAPModification;
import org.ietf.ldap.LDAPAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class ModifyHandler {

    Logger log = LoggerFactory.getLogger(getClass());

    private Handler handler;
    private HandlerContext handlerContext;

    public ModifyHandler(Handler handler) throws Exception {
        this.handler = handler;
        this.handlerContext = handler.getHandlerContext();
    }

    public int modify(PenroseConnection connection, String dn, List modifications)
			throws Exception {

        log.info("-------------------------------------------------");
		log.info("MODIFY:");
		if (connection.getBindDn() != null) log.info(" - bindDn: " + connection.getBindDn());
        log.info(" - dn: " + dn);
        log.debug("-------------------------------------------------");
		log.debug("changetype: modify");

		for (Iterator i = modifications.iterator(); i.hasNext();) {
			LDAPModification modification = (LDAPModification) i.next();

			LDAPAttribute attribute = modification.getAttribute();
			String attributeName = attribute.getName();
			String values[] = attribute.getStringValueArray();

			switch (modification.getOp()) {
			case LDAPModification.ADD:
				log.debug("add: " + attributeName);
				for (int j = 0; j < values.length; j++)
					log.debug(attributeName + ": " + values[j]);
				break;
			case LDAPModification.DELETE:
				log.debug("delete: " + attributeName);
				for (int j = 0; j < values.length; j++)
					log.debug(attributeName + ": " + values[j]);
				break;
			case LDAPModification.REPLACE:
				log.debug("replace: " + attributeName);
				for (int j = 0; j < values.length; j++)
					log.debug(attributeName + ": " + values[j]);
				break;
			}
			log.debug("-");
		}

        log.info("");

        ModifyEvent beforeModifyEvent = new ModifyEvent(this, ModifyEvent.BEFORE_MODIFY, connection, dn, modifications);
        handler.postEvent(dn, beforeModifyEvent);

        int rc = performModify(connection, dn, modifications);

        ModifyEvent afterModifyEvent = new ModifyEvent(this, ModifyEvent.AFTER_MODIFY, connection, dn, modifications);
        afterModifyEvent.setReturnCode(rc);
        handler.postEvent(dn, afterModifyEvent);

        return rc;
    }

    public int performModify(PenroseConnection connection, String dn, List modifications)
			throws Exception {

		String ndn = LDAPDN.normalize(dn);

        Entry entry = null;
        try {
            entry = handler.getSearchHandler().find(connection, ndn);
        } catch (LDAPException e) {
            // ignore
        }

        if (entry == null) return LDAPException.NO_SUCH_OBJECT;

        int rc = handlerContext.getACLEngine().checkModify(connection, entry);
        if (rc != LDAPException.SUCCESS) return rc;

        EntryDefinition entryDefinition = entry.getEntryDefinition();
        if (entryDefinition.isDynamic()) {
            return modifyVirtualEntry(connection, entry, modifications);

        } else {
            return modifyStaticEntry(entryDefinition, modifications);
        }
	}

    /**
     * Apply encryption/encoding at attribute level.
     * @param entry
     * @param modifications
     * @throws Exception
     */
	public void convertValues(EntryDefinition entry, Collection modifications)
			throws Exception {

		for (Iterator i = modifications.iterator(); i.hasNext();) {
			LDAPModification modification = (LDAPModification) i.next();

			LDAPAttribute attribute = modification.getAttribute();
			String attributeName = attribute.getName();
			String values[] = attribute.getStringValueArray();

			AttributeDefinition attr = entry.getAttributeDefinition(attributeName);
			if (attr == null) continue;

			String encryption = attr.getEncryption();
			String encoding = attr.getEncoding();

			for (int j = 0; j < values.length; j++) {
				log.debug("old " + attributeName + ": " + values[j]);
				attribute.removeValue(values[j]);
				values[j] = PasswordUtil.encrypt(encryption, encoding,
						values[j]);
				log.debug("new " + attributeName + ": " + values[j]);
				attribute.addValue(values[j]);
			}
		}
	}

    public int modifyVirtualEntry(
            PenroseConnection connection,
            Entry entry,
			Collection modifications)
            throws Exception {

		EntryDefinition entryDefinition = entry.getEntryDefinition();
        AttributeValues oldValues = entry.getAttributeValues();

		convertValues(entryDefinition, modifications);

		log.debug("--- old values:");
		log.debug(entry.toString());

		log.debug("--- perform modification:");
		AttributeValues newValues = new AttributeValues(oldValues);

        Schema schema = handlerContext.getSchema();
		Collection objectClasses = schema.getObjectClasses(entryDefinition);
		//log.debug("Object Classes: " + objectClasses);

		for (Iterator i = modifications.iterator(); i.hasNext();) {
			LDAPModification modification = (LDAPModification) i.next();

			LDAPAttribute attribute = modification.getAttribute();
			String attributeName = attribute.getName();

			if (attributeName.equals("entryCSN"))
				continue; // ignore
			if (attributeName.equals("modifiersName"))
				continue; // ignore
			if (attributeName.equals("modifyTimestamp"))
				continue; // ignore

			if (attributeName.equals("objectClass"))
				return LDAPException.OBJECT_CLASS_MODS_PROHIBITED;

			// check if the attribute is defined in the object class

			boolean found = false;
			for (Iterator j = objectClasses.iterator(); j.hasNext();) {
				ObjectClass oc = (ObjectClass) j.next();
				log.debug("Object Class: " + oc.getName());
				log.debug(" - required: " + oc.getRequiredAttributes());
				log.debug(" - optional: " + oc.getOptionalAttributes());

				if (oc.getRequiredAttributes().contains(attributeName)
						|| oc.getOptionalAttributes().contains(attributeName)) {
					found = true;
					break;
				}
			}

			if (!found) {
				log.debug("Can't find attribute " + attributeName
						+ " in object classes");
				return LDAPException.OBJECT_CLASS_VIOLATION;
			}

			String attributeValues[] = attribute.getStringValueArray();
			Set newAttrValues = new HashSet();
			for (int j = 0; j < attributeValues.length; j++) {
				newAttrValues.add(attributeValues[j]);
			}

			Collection value = newValues.get(attributeName);
			log.debug("old value " + attributeName + ": "
					+ newValues.get(attributeName));

			Set newValue = new HashSet();
            if (value != null) newValue.addAll(value);

			switch (modification.getOp()) {
			case LDAPModification.ADD:
				newValue.addAll(newAttrValues);
				break;
			case LDAPModification.DELETE:
				if (attributeValues.length == 0) {
					newValue.clear();
				} else {
					newValue.removeAll(newAttrValues);
				}
				break;
			case LDAPModification.REPLACE:
				newValue = newAttrValues;
				break;
			}

			newValues.set(attributeName, newValue);

			log.debug("new value " + attributeName + ": "
					+ newValues.get(attributeName));
		}

        Entry newEntry = new Entry(entry.getDn(), entryDefinition, entry.getSourceValues(), newValues);
        newEntry.setParent(entry.getParent());

		log.debug("--- new values:");
		log.debug(newEntry.toString());

        return handlerContext.getEngine().modify(entry, oldValues, newValues);
	}

    public int modifyStaticEntry(EntryDefinition entry, Collection modifications)
			throws Exception {

		convertValues(entry, modifications);

		for (Iterator i = modifications.iterator(); i.hasNext();) {
			LDAPModification modification = (LDAPModification) i.next();

			LDAPAttribute attribute = modification.getAttribute();
			String attributeName = attribute.getName();

			String attributeValues[] = attribute.getStringValueArray();

			switch (modification.getOp()) {
			case LDAPModification.ADD:
				for (int j = 0; j < attributeValues.length; j++) {
					String v = "\"" + attributeValues[j] + "\"";
					addAttribute(entry, attributeName, v);
				}
				break;

			case LDAPModification.DELETE:
				if (attributeValues.length == 0) {
					deleteAttribute(entry, attributeName);

				} else {
					for (int j = 0; j < attributeValues.length; j++) {
						String v = "\"" + attributeValues[j] + "\"";
						deleteAttribute(entry, attributeName, v);
					}
				}
				break;
			case LDAPModification.REPLACE:
				deleteAttribute(entry, attributeName);

				for (int j = 0; j < attributeValues.length; j++) {
					String v = "\"" + attributeValues[j] + "\"";
					addAttribute(entry, attributeName, v);
				}
				break;
			}
		}

		/*
		 * for (Iterator i = attributes.iterator(); i.hasNext(); ) { AttributeDefinition
		 * attribute = (AttributeDefinition)i.next(); log.debug(attribute.getName()+":
		 * "+attribute.getExpression()); }
		 */
		return LDAPException.SUCCESS;
	}

    public void addAttribute(EntryDefinition entry, String name, String value)
			throws Exception {

		AttributeDefinition attribute = entry.getAttributeDefinition(name);

		if (attribute == null) {
			attribute = new AttributeDefinition(name, value);
			entry.addAttributeDefinition(attribute);

		} else {
            // if already exists, don't add
			if (attribute.getExpression().getScript().equals(value)) return;

			attribute.getExpression().setScript(value);
		}
	}

    public void deleteAttribute(EntryDefinition entry, String name) throws Exception {
		entry.removeAttributeDefinition(name);
	}

    public void deleteAttribute(EntryDefinition entry, String name, String value)
			throws Exception {

		AttributeDefinition attribute = entry.getAttributeDefinition(name);
		if (attribute == null) return;

		Interpreter interpreter = handlerContext.newInterpreter();

		String attrValue = (String)interpreter.eval(attribute.getExpression().getScript());
		if (attrValue.equals(value)) entry.removeAttributeDefinition(name);
	}

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public HandlerContext getHandlerContext() {
        return handlerContext;
    }

    public void setHandlerContext(HandlerContext handlerContext) {
        this.handlerContext = handlerContext;
    }
}
