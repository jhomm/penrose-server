/**
 * Copyright 2009 Red Hat, Inc.
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
package org.safehaus.penrose.jdbc.adapter;

import org.safehaus.penrose.adapter.Adapter;
import org.safehaus.penrose.jdbc.connection.*;
import org.safehaus.penrose.jdbc.source.JDBCSource;

/**
 * @author Endi S. Dewata
 */
public class JDBCAdapter extends Adapter {

    public boolean isJoinSupported() {
        return true;
    }

    public String getConnectionClassName() {
        return JDBCConnection.class.getName();
    }

    public String getSourceClassName() throws Exception {
        return JDBCSource.class.getName();
    }

}
