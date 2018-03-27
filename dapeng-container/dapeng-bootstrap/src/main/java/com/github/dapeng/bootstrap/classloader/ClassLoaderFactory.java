package com.github.dapeng.bootstrap.classloader;

import java.util.List;

public class ClassLoaderFactory {

    private static CoreClassLoader coreClassLoader;
    private static ClassLoader platformClassLoader;
    private static List<ClassLoader> pluginClassLoaders;

    public static final void setCoreClassLoader(CoreClassLoader coreClassLoader) {
        ClassLoaderFactory.coreClassLoader = coreClassLoader;
    }

    public static final void setPlatformClassLoader(ClassLoader platformClassLoader) {
        ClassLoaderFactory.platformClassLoader = platformClassLoader;
    }


    public static void setPluginClassLoaders(List<ClassLoader> pluginClassLoaders) {
        ClassLoaderFactory.pluginClassLoaders = pluginClassLoaders;
    }

    public static List<ClassLoader> getPluginClassLoaders() {
        return pluginClassLoaders;
    }

    public static CoreClassLoader getCoreClassLoader() {
        return coreClassLoader;
    }

    public static ClassLoader getPlatformClassLoader() {
        return platformClassLoader;
    }
}
