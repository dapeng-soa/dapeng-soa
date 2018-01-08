package com.github.dapeng.bootstrap.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Share Class Loader
 *
 * @author craneding
 * @date 16/1/28
 */
public class CoreClassLoader extends URLClassLoader {

    public CoreClassLoader(URL[] urls) {
        super(urls, Thread.currentThread().getContextClassLoader());
    }

}
