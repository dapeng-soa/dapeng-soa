/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.bootstrap;

import com.github.dapeng.bootstrap.classloader.ApplicationClassLoader;
import com.github.dapeng.bootstrap.classloader.ContainerClassLoader;
import com.github.dapeng.bootstrap.classloader.CoreClassLoader;
import com.github.dapeng.bootstrap.classloader.PluginClassLoader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Bootstrap {
    private static final String ENGINE_PATH = System.getProperty("soa.base", new File(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().getParentFile().getParent() + "/dapeng-container-impl/target/dapeng-container/");


    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<URL> coreURLs = findJarURLs(new File(ENGINE_PATH, "lib"));
        List<URL> containerURLs = findJarURLs(new File(ENGINE_PATH, "bin/lib"));
        List<List<URL>> applicationURLs = getUrlList(new File(ENGINE_PATH, "apps"));
        List<List<URL>> pluginURLs = getUrlList(new File(ENGINE_PATH, "plugin"));

        CoreClassLoader coreClassLoader = new CoreClassLoader(coreURLs.toArray(new URL[coreURLs.size()]));

        ClassLoader containerClassLoader = new ContainerClassLoader(containerURLs.toArray(new URL[containerURLs.size()]), coreClassLoader);

        List<ClassLoader> applicationCls = applicationURLs.stream().map(i -> new ApplicationClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());

        List<ClassLoader> pluginClassLoaders = pluginURLs.stream().map(i -> new PluginClassLoader(i.toArray(new URL[i.size()]), containerClassLoader)).collect(Collectors.toList());

        startup(containerClassLoader, applicationCls, pluginClassLoaders);
    }

    /**
     * @param containerClassLoader
     * @param applicationLibs
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static void sbtStartup(ClassLoader containerClassLoader, List<URL> applicationLibs, List<List<URL>> pluginsLibs) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        ClassLoader coreCL = containerClassLoader;
        List<ClassLoader> pluginCls = null;

        ClassLoader applicationCL = new ApplicationClassLoader(
                applicationLibs.toArray(new URL[applicationLibs.size()]),
                null,
                coreCL);
        List<ClassLoader> applicationCLs = new ArrayList<>();
        applicationCLs.add(applicationCL);

        if (pluginsLibs != null) {
            pluginCls = new ArrayList<>();
            for (List<URL> pluginLibs : pluginsLibs) {
                ClassLoader pluginClassLoader = new PluginClassLoader(pluginLibs.toArray(new URL[pluginLibs.size()]), containerClassLoader);
                pluginCls.add(pluginClassLoader);
            }
        }

        startup(containerClassLoader, applicationCLs, pluginCls);

    }


    public static void startup(ClassLoader containerCl, List<ClassLoader> applicationCls, List<ClassLoader> pluginCls) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Thread.currentThread().setContextClassLoader(containerCl);
        Class<?> containerFactoryClz = containerCl.loadClass("com.github.dapeng.api.ContainerFactory");
        Method createContainerMethod = containerFactoryClz.getMethod("createContainer", List.class, ClassLoader.class, List.class);
        createContainerMethod.invoke(containerFactoryClz, applicationCls, containerCl, pluginCls);

        Method getContainerMethod = containerFactoryClz.getMethod("getContainer");
        Object container = getContainerMethod.invoke(containerFactoryClz);

        Method mainMethod = container.getClass().getMethod("startup");
        mainMethod.invoke(container);
    }

    private static List<List<URL>> getUrlList(File filepath) throws MalformedURLException {
        List<List<URL>> urlsList = new ArrayList<>();
        if (filepath.exists() && filepath.isDirectory()) {
            final File[] files = filepath.listFiles();
            for (File file : files) {
                final List<URL> urlList = new ArrayList<>();
                if (file.isDirectory()) {
                    urlList.addAll(findJarURLs(file));
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    urlList.add(file.toURI().toURL());
                }
                if (!urlList.isEmpty()) {
                    urlsList.add(urlList);
                }
            }
        }
        return urlsList;
    }

    private static List<URL> findJarURLs(File file) throws MalformedURLException {
        final List<URL> urlList = new ArrayList<>();

        if (file != null && file.exists()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                urlList.add(file.toURI().toURL());
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File file1 : files) {
                        urlList.addAll(findJarURLs(file1));
                    }
                }
            }
        }

        return urlList;
    }
}
