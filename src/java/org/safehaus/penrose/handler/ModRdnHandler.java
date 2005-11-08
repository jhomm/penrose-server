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
import org.safehaus.penrose.config.Config;
import org.safehaus.penrose.mapping.Entry;
import org.safehaus.penrose.mapping.EntryDefinition;
import org.safehaus.penrose.mapping.AttributeValues;
import org.apache.log4j.Logger;
import org.ietf.ldap.LDAPException;
import org.ietf.ldap.LDAPDN;

import java.util.List;
import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class ModRdnHandler {

    Logger log = Logger.getLogger(getClass());

    public Handler handler;
    private HandlerContext handlerContext;

	public ModRdnHandler(Handler handler) throws Exception {
        this.handler = handler;
        this.handlerContext = handler.getHandlerContext();
	}

	public int modrdn(PenroseConnection connection, String dn, String newRdn)
			throws Exception {

        log.debug("-------------------------------------------------------------------------------");
        log.debug("MODRDN:");
        if (connection.getBindDn() != null) log.info(" - Bind DN: " + connection.getBindDn());
        log.debug(" - DN: " + dn);
        log.debug(" - New RDN: " + newRdn);

        //return LDAPException.LDAP_NOT_SUPPORTED;

        int rc = performModRdn(connection, dn, newRdn);

        return rc;
	}

    public int performModRdn(
            PenroseConnection connection,
            String dn,
            String newRdn)
			throws Exception {

		String ndn = LDAPDN.normalize(dn);

        Entry entry = handler.getSearchHandler().find(connection, ndn);
        if (entry == null) return LDAPException.NO_SUCH_OBJECT;

        int rc = handlerContext.getACLEngine().checkModify(connection, entry);
        if (rc != LDAPException.SUCCESS) return rc;

        EntryDefinition entryDefinition = entry.getEntryDefinition();
        if (entryDefinition.isDynamic()) {
            return modRdnVirtualEntry(connection, entry, newRdn);

        } else {
            return modRdnStaticEntry(entryDefinition, newRdn);
        }
	}

    public int modRdnStaticEntry(
            EntryDefinition entry,
            String newRdn)
			throws Exception {

        Config config = handlerContext.getConfig(entry.getDn());
        config.renameEntryDefinition(entry, newRdn);

        return LDAPException.SUCCESS;
    }

    public int modRdnVirtualEntry(
            PenroseConnection connection,
            Entry entry,
			String newRdn)
            throws Exception {

        return handlerContext.getEngine().modrdn(entry, newRdn);
    }
}
