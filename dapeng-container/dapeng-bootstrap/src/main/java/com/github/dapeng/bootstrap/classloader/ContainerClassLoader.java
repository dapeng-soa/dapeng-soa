package com.github.dapeng.bootstrap.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Platform Class Loader
 *
 * @author craneding
 * @date 16/1/28
 */
public class ContainerClassLoader extends URLClassLoader {

    public ContainerClassLoader(URL[] urls, ClassLoader coreClassLoader) {
        super(urls, coreClassLoader);
    }

}
