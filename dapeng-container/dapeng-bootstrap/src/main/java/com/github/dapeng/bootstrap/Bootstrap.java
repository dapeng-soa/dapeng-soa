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
    private static final String ENGINE_PATH = initialzie();
    private static final String DEFAULT_APPS_DIR = ENGINE_PATH + "/apps";

    private static String initialzie() {
        String soaBase = getEnginePath();
        if (System.getProperty("soa.container.logging.path") == null) {
            System.setProperty("soa.container.logging.path", soaBase + "/logs/");
        }
        return soaBase;
    }

    private static String getEnginePath() {

        String base = System.getProperty("soa.base");
        if (base != null) return base;

        File classPath = new File(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        if (classPath.isDirectory()) {
            // in DEV mode, root/dapeng-container/dapeng-bootstrap/target/classes
            base = classPath.getParentFile()    // root/dapeng-container/dapeng-bootstrap/target
                    .getParentFile()    // root/dapeng-container/dapeng-bootstrap/
                    .getParentFile()    // root/dapeng-container/
                    .getPath() + "/dapeng-container-impl/target/dapeng-container";
        } else if (classPath.getName().endsWith(".jar")) { // HOME/bin/dapeng-bootstrap.jar
            base = classPath.getParentFile() // HOME/bin
                    .getParentFile() // HOME
                    .getPath();
        } else {
            throw new RuntimeException("please specify engine's home via -Dsoa.base");
        }
        System.setProperty("soa.base", base);
        return base;
    }

    /**
     * -apps apps_dir
     * -app app_dir|app_jar
     */
    public static void main(String args[]) throws ClassNotFoundException, MalformedURLException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String appsDir = null;
        List<String> appFiles = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if ("-apps".equals(args[i]) && (i + 1) < args.length) {
                if (appsDir != null) {
                    throw new RuntimeException("too many -apps options");
                } else {
                    appsDir = args[i + 1];
                    i++;
                    continue;
                }
            } else if ("-app".equals(args[i]) && (i + 1) < args.length) {
                appFiles.add(args[i + 1]);
                i++;
                continue;
            } else if ("-logdir".equals(args[i]) && (i + 1) < args.length) {
                System.setProperty("soa.container.logging.path", args[i + 1]);
                i++;
                continue;
            }
        }

        if (appsDir != null) {
            getApps(appsDir, appFiles);
        }

        if (appFiles.isEmpty()) {
            // try the default apps dir
            getApps(DEFAULT_APPS_DIR, appFiles);
        }

        launch(appFiles);

    }

    private static void getApps(String appsDir, List<String> appFiles) {
        File dir = new File(appsDir);
        if (!dir.isDirectory()) throw new RuntimeException("apps " + appsDir + " is not a directory");
        for (File app : dir.listFiles()) {
            if (app.isDirectory()) appFiles.add(app.getPath());
            else if (app.isFile() && app.getPath().endsWith(".jar")) appFiles.add(app.getPath());
        }
    }

    static void launch(List<String> apps) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<URL> coreURLs = findJarURLs(new File(ENGINE_PATH, "lib"));
        List<URL> containerURLs = findJarURLs(new File(ENGINE_PATH, "bin/lib"));
        List<List<URL>> pluginURLs = getUrlList(new File(ENGINE_PATH, "plugin"));
        List<List<URL>> applicationURLs = findAppJars(apps);

        CoreClassLoader coreClassLoader = new CoreClassLoader(coreURLs.toArray(new URL[coreURLs.size()]));

        ClassLoader platformClassLoader = new ContainerClassLoader(containerURLs.toArray(new URL[containerURLs.size()]), coreClassLoader);

        List<ClassLoader> applicationCls = applicationURLs.stream().map(i -> new ApplicationClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());

        List<ClassLoader> pluginClassLoaders = pluginURLs.stream().map(i -> new PluginClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());

        startup(platformClassLoader, applicationCls);
    }

//    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        List<URL> coreURLs = findJarURLs(new File(ENGINE_PATH, "lib"));
//        List<URL> containerURLs = findJarURLs(new File(ENGINE_PATH, "bin/lib"));
//        List<List<URL>> applicationURLs = getUrlList(new File(ENGINE_PATH, "apps"));
//        List<List<URL>> pluginURLs = getUrlList(new File(ENGINE_PATH, "plugin"));
//
//        CoreClassLoader coreClassLoader = new CoreClassLoader(coreURLs.toArray(new URL[coreURLs.size()]));
//
//        ClassLoader platformClassLoader = new ContainerClassLoader(containerURLs.toArray(new URL[containerURLs.size()]), coreClassLoader);
//
//        List<ClassLoader> applicationCls = applicationURLs.stream().map(i -> new ApplicationClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());
//
//        List<ClassLoader> pluginClassLoaders = pluginURLs.stream().map(i -> new PluginClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());
//
//        startup(platformClassLoader, applicationCls);
//    }

    public static void sbtStartup(ClassLoader containerClassLoader, List<URL> applicationLibs) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        ClassLoader coreCL = containerClassLoader;
        ClassLoader containerCL = containerClassLoader;
        System.setProperty("soa.run.mode", "plugin");   // enable ApiDoc

        ClassLoader applicationCL = new ApplicationClassLoader(
                applicationLibs.toArray(new URL[applicationLibs.size()]),
                null,
                coreCL);
        List<ClassLoader> applicationCLs = new ArrayList<>();
        applicationCLs.add(applicationCL);

        startup(containerCL, applicationCLs);

    }


    public static void startup(ClassLoader containerCl, List<ClassLoader> applicationCls) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Thread.currentThread().setContextClassLoader(containerCl);
        Class<?> containerFactoryClz = containerCl.loadClass("com.github.dapeng.api.ContainerFactory");
        Method createContainerMethod = containerFactoryClz.getMethod("createContainer", List.class, ClassLoader.class);
        createContainerMethod.invoke(containerFactoryClz, applicationCls, containerCl);

        Method getContainerMethod = containerFactoryClz.getMethod("getContainer");
        Object container = getContainerMethod.invoke(containerFactoryClz);

        Method mainMethod = container.getClass().getMethod("startup");
        mainMethod.invoke(container);
    }

    /**
     * @param apps each app is ethier a single jar or a directory
     * @return
     * @throws MalformedURLException
     */
    private static List<List<URL>> findAppJars(List<String> apps) throws MalformedURLException {
        List<List<URL>> result = new ArrayList<>();
        for (String app : apps) {
            File appFile = new File(app);
            if (appFile.isFile() && appFile.getPath().endsWith(".jar")) {
                List<URL> jars = new ArrayList<>();
                jars.add(appFile.toURI().toURL());
                result.add(jars);
                continue;
            } else if (appFile.isDirectory()) {
                List<URL> jars = new ArrayList<>();
                jars.addAll(findJarURLs(appFile));
                if (!jars.isEmpty())
                    result.add(jars);
            }
        }
        return result;
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
