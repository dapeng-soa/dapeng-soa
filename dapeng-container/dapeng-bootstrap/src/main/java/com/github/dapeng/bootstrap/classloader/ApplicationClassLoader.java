package com.github.dapeng.bootstrap.classloader;

import com.github.dapeng.bootstrap.Bootstrap;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * App Class Loader
 *
 * @author craneding
 * @date 16/1/28
 */
public class ApplicationClassLoader extends URLClassLoader {

    private final ClassLoader coreClassLoader;

    public ApplicationClassLoader(URL[] urls, ClassLoader coreClassLoader) {
        super(urls, ClassLoader.getSystemClassLoader());
        this.coreClassLoader = coreClassLoader;
    }

    public ApplicationClassLoader(URL[] urls, ClassLoader parent, ClassLoader coreClassLoader) {
        super(urls, parent);
        this.coreClassLoader = coreClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        if (name.startsWith("com.github.dapeng.core")
                || name.startsWith("com.github.dapeng.org.apache.thrift")
                || name.startsWith("com.github.dapeng.transaction.api")
                || name.startsWith("com.google.gson")
                || name.startsWith("org.apache.skywalking.apm")) {
            return coreClassLoader.loadClass(name);
        }

        Class clz =  super.loadClass(name, resolve);
        return clz;
    }
}
