package org.safehaus.penrose.util;

import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map;

public class ClassRegistry extends ClassLoader {

    Logger log = Logger.getLogger(getClass());

    Map classes = new TreeMap();

    public ClassRegistry() {
        this(null);
    }

    public ClassRegistry(ClassLoader parent) {
        super(parent);
    }

    public Class findClass(String name) throws ClassNotFoundException {
        Class clazz = (Class)classes.get(name);
        if (clazz != null) return clazz;

        byte[] bytes = loadClassData(name);
        clazz = defineClass(name, bytes, 0, bytes.length);
        classes.put(name, clazz);
        
        return clazz;
    }

    protected byte[] loadClassData(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    protected byte[] merge(Collection list) {

        int length = 0;
        for (Iterator i=list.iterator(); i.hasNext(); ) {
            byte[] b = (byte[])i.next();
            length += b.length;
        }

        //log.debug("Preparing "+length+" bytes array");
        byte[] bytes = new byte[length];

        int counter = 0;
        for (Iterator i=list.iterator(); i.hasNext(); ) {
            byte[] b = (byte[])i.next();
            //log.debug("Copying "+b.length+" bytes");
            for (int j=0; j<b.length; j++) bytes[counter++] = b[j];
        }

        return bytes;
    }
}