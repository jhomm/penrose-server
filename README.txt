${project.title} Server
--------------
Version ${product.version}
Copyright 2009 Red Hat, Inc.

Overview
--------

${project.title} is an open source java-based virtual directory server. A virtual directory does not store
any information itself, unlike other LDAP implementations. Requests received from LDAP client
applications are processed by the virtual directory server and passed on to the data source hosting
the desired data. Frequently this data source will be a relational database, and more often than not
it will be the authoritative source of the directory information.

Documentation
-------------

Please find ${project.title} documentation online at http://penrose.safehaus.org/Documentation.

Getting the Source Code
-----------------------

Checkout the project from:

   svn co https://svn.safehaus.org/repos/penrose/trunk

Building
--------

To build ${project.title} execute the following command:

   ant dist

The distribution files can be found under the target directory.

--------------------------
2017.06.03 [jhomm]
The target dist-all or dist-win32 requires inno setup installed on build server. If missing the build failes with error message
'Cannot run program "ISCC.exe"...'
 