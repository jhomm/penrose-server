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
import org.safehaus.penrose.event.BindEvent;
import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.util.PasswordUtil;
import org.ietf.ldap.LDAPDN;
import org.ietf.ldap.LDAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class BindHandler {

    Logger log = LoggerFactory.getLogger(getClass());

    private Handler handler;
    private HandlerContext handlerContext;

    public BindHandler(Handler handler) throws Exception {
        this.handler = handler;
        this.handlerContext = handler.getHandlerContext();
    }

    public int bind(PenroseConnection connection, String dn, String password) throws Exception {

        log.info("-------------------------------------------------");
        log.info("BIND:");
        log.info(" - dn      : "+dn);

        BindEvent beforeBindEvent = new BindEvent(this, BindEvent.BEFORE_BIND, connection, dn, password);
        handler.postEvent(dn, beforeBindEvent);

        int rc = performBind(connection, dn, password);

        BindEvent afterBindEvent = new BindEvent(this, BindEvent.AFTER_BIND, connection, dn, password);
        afterBindEvent.setReturnCode(rc);
        handler.postEvent(dn, afterBindEvent);

        return rc;
    }

    public int performBind(PenroseConnection connection, String dn, String password) throws Exception {

        String ndn = LDAPDN.normalize(dn);

        if (handlerContext.getRootDn() != null && ndn.equals(LDAPDN.normalize(handlerContext.getRootDn()))) { // bind as root

            int rc = bindAsRoot(password);
            if (rc != LDAPException.SUCCESS) return rc;

        } else {

            int rc = bindAsUser(connection, ndn, password);
            if (rc != LDAPException.SUCCESS) return rc;
        }

        connection.setBindDn(dn);
        return LDAPException.SUCCESS; // LDAP_SUCCESS
    }

    public int unbind(PenroseConnection connection) throws Exception {

        log.debug("-------------------------------------------------------------------------------");
        log.debug("UNBIND:");

        if (connection == null) return 0;

        connection.setBindDn(null);

        log.debug("  dn: " + connection.getBindDn());

        return 0;
    }

    public int bindAsRoot(String password) throws Exception {
        log.debug("Comparing root's password");

        if (!PasswordUtil.comparePassword(password, handlerContext.getRootPassword())) {
            log.debug("Password doesn't match => BIND FAILED");
            return LDAPException.INVALID_CREDENTIALS;
        }

        return LDAPException.SUCCESS;
    }

    public int bindAsUser(PenroseConnection connection, String dn, String password) throws Exception {
        log.debug("Searching for "+dn);

        List attributeNames = new ArrayList();
        attributeNames.add("userPassword");
        Entry sr = null;
        try {
            sr = handler.getSearchHandler().find(connection, dn);
        } catch (Exception e) {
            // ignore
        }

        //if (sr == null) return LDAPException.NO_SUCH_OBJECT;
        if (sr == null) {
            log.debug("Entry "+dn+" not found => BIND FAILED");
            return LDAPException.INVALID_CREDENTIALS;
        }

        log.debug("Found "+sr.getDn());

        return bindAsUser(connection, sr, password);
    }

    public int bindAsUser(PenroseConnection connection, Entry sr, String password) throws Exception {

        log.debug("Bind as user "+sr.getDn());

        EntryDefinition entry = sr.getEntryDefinition();
        AttributeValues attributes = sr.getAttributeValues();

        Collection set = attributes.get("userPassword");

        if (set != null && !set.isEmpty()) {
            log.debug("Entry has userPassword");
            return bindAsStaticUser(sr, password);
        }

        return bind(entry, attributes, password);
    }

    public int bindAsStaticUser(Entry sr, String cred) throws Exception {
        EntryDefinition entry = sr.getEntryDefinition();
        AttributeValues values = sr.getAttributeValues();

        AttributeDefinition attribute = entry.getAttributeDefinition("userPassword");
        String encryption = attribute.getEncryption();
        String encoding = attribute.getEncoding();

        Collection set = values.get("userPassword");

        for (Iterator i = set.iterator(); i.hasNext(); ) {
            String userPassword = (String)i.next();
            log.debug("userPassword: "+userPassword);
            if (PasswordUtil.comparePassword(cred, encryption, encoding, userPassword)) return LDAPException.SUCCESS;
        }

        return LDAPException.INVALID_CREDENTIALS;
    }

    public int bind(EntryDefinition entry, AttributeValues values, String password) throws Exception {

        Collection sources = entry.getSources();

        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            Source source = (Source)i.next();

            int rc = getEngineContext().getSyncService().bind(source, entry, values, password);

            if (rc == LDAPException.SUCCESS) return rc;
        }

        return LDAPException.INVALID_CREDENTIALS;
    }

    public Handler getEngine() {
        return handler;
    }

    public void setEngine(Handler handler) {
        this.handler = handler;
    }

    public HandlerContext getEngineContext() {
        return handlerContext;
    }

    public void setEngineContext(HandlerContext handlerContext) {
        this.handlerContext = handlerContext;
    }
}
