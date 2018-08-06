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
    public static final String ENGINE_PATH = System.getProperty("soa.base", new File(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().getParentFile().getParent() + "/dapeng-container-impl/target/dapeng-container/");


    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<URL> coreURLs = findJarURLs(new File(ENGINE_PATH, "lib"));
        List<URL> containerURLs = findJarURLs(new File(ENGINE_PATH, "bin/lib"));
        List<List<URL>> applicationURLs = getUrlList(new File(ENGINE_PATH, "apps"));
        List<List<URL>> pluginURLs = getUrlList(new File(ENGINE_PATH, "plugin"));

        CoreClassLoader coreClassLoader = new CoreClassLoader(coreURLs.toArray(new URL[coreURLs.size()]));

        ClassLoader platformClassLoader = new ContainerClassLoader(containerURLs.toArray(new URL[containerURLs.size()]), coreClassLoader);

        List<ClassLoader> applicationCls = applicationURLs.stream().map(i -> new ApplicationClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());

        List<ClassLoader> pluginClassLoaders = pluginURLs.stream().map(i -> new PluginClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());

        startup(platformClassLoader, applicationCls);
    }

    public static void sbtStartup(ClassLoader containerClassLoader, List<URL> applicationLibs) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        ClassLoader coreCL = containerClassLoader;
        ClassLoader containerCL = containerClassLoader;

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

        Class<?> doctorFactoryClz = containerCl.loadClass("com.github.dapeng.api.healthcheck.DoctorFactory");
        Method createDoctorMethod = doctorFactoryClz.getMethod("createDoctor", List.class, ClassLoader.class);
        createDoctorMethod.invoke(doctorFactoryClz, applicationCls, containerCl);

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

    public static List<URL> findJarURLs(File file) throws MalformedURLException {
        final List<URL> urlList = new ArrayList<>();

        if (file != null && file.exists()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                urlList.add(file.toURI().toURL());
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        urlList.addAll(findJarURLs(files[i]));
                    }
                }
            }
        }

        return urlList;
    }
}
