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
package com.github.dapeng.maven.plugin;

import com.github.dapeng.bootstrap.Bootstrap;
import com.github.dapeng.bootstrap.classloader.ApplicationClassLoader;
import com.github.dapeng.bootstrap.classloader.ContainerClassLoader;
import com.github.dapeng.bootstrap.classloader.CoreClassLoader;
import com.github.dapeng.bootstrap.classloader.PluginClassLoader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Run Container Plugin
 *
 * @author craneding
 * @date 16/1/25
 */
@Mojo(name = "run", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class RunContainerPlugin extends SoaAbstractMojo {

    @Parameter(property = "pluginPath")
    private String pluginPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project == null) {
            throw new MojoExecutionException("not found project.");
        }

        getLog().info("bundle:" + project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());

        System.setProperty("soa.base", new File(project.getBuild().getOutputDirectory()).getAbsolutePath().replace("/target/classes", ""));

        System.setProperty("soa.run.mode", "plugin");

        IsolatedThreadGroup threadGroup = new IsolatedThreadGroup("RunContainerPlugin");
        Thread bootstrapThread = new Thread(threadGroup, () -> {
            try {

                URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();

                List<URL> shareUrls = new ArrayList<>(Arrays.asList(urls));
                Iterator<URL> iterator = shareUrls.iterator();
                while (iterator.hasNext()) {
                    URL url = iterator.next();

                    if (url.getFile().matches("^.*/dapeng-transaction-impl.*\\.jar$")) {
                        iterator.remove();
                        continue;
                    }
                }

                List<URL> platformUrls = new ArrayList<>(Arrays.asList(urls));
                iterator = platformUrls.iterator();
                while (iterator.hasNext()) {
                    URL url = iterator.next();
                    if (removeServiceProjectArtifact(iterator, url)) continue;
                    //if (removeTwitterAndScalaDependency(iterator,url)) continue;
                }

                List<URL> appUrls = new ArrayList<>(Arrays.asList(urls));
                iterator = appUrls.iterator();

                while (iterator.hasNext()) {
                    URL url = iterator.next();
                    if (removeTwitterAndScalaDependency(iterator, url)) continue;
                    if (removeContainerAndBootstrap(iterator, url)) continue;
                }

                List<List<URL>> appURLsList = new ArrayList<>();
                appURLsList.add(appUrls);

                CoreClassLoader coreClassLoader = new CoreClassLoader(shareUrls.toArray(new URL[shareUrls.size()]));

                List<ClassLoader> appClassLoaders = appURLsList.stream().map(i ->
                        new ApplicationClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());

                ContainerClassLoader platformClassLoader = new ContainerClassLoader(platformUrls.toArray(new URL[platformUrls.size()]), coreClassLoader);

                getLog().info("------set classloader-------------");
                Thread.currentThread().setContextClassLoader(coreClassLoader);


                //20190712   添加形势任务插件 classLoader
                List<ClassLoader> pluginCls = null;
                if (pluginPath != null) {
                    getLog().info("*-*-*-*-*-*-*-*-*-*container load plugin path:["+pluginPath+"]*-*-*-*-*-*-*-*-*-*");
                    List<List<URL>> pluginsLibs = getPluginsLibs(pluginPath);
                    if (pluginsLibs != null) {
                        pluginCls = new ArrayList<>();
                        for (List<URL> pluginLibs : pluginsLibs) {
                            ClassLoader pluginClassLoader = new PluginClassLoader(pluginLibs.toArray(new URL[pluginLibs.size()]), platformClassLoader);
                            pluginCls.add(pluginClassLoader);
                        }
                    }
                }

                //todo
                Bootstrap.startup(platformClassLoader, appClassLoaders, pluginCls);

            } catch (Exception e) {
                Thread.currentThread().getThreadGroup().uncaughtException(Thread.currentThread(), e);
            }
        }, "RunContainerPlugin" + ".main()");
        bootstrapThread.setContextClassLoader(getClassLoader());
        bootstrapThread.start();

        joinNonDaemonThreads(threadGroup);
    }

    private List<List<URL>> getPluginsLibs(String pluginPath) throws Exception {
        List<List<URL>> pluginsLibs = new ArrayList<List<URL>>();
        if (pluginPath != null) {
            File pluginParentDir = new File(pluginPath);
            if (!pluginParentDir.exists()) {
                throw new FileNotFoundException("文件目录不存在: [" + pluginPath + "]");
            }

            File[] pluginDirs = pluginParentDir.listFiles();
            if (pluginDirs != null) {
                for (File pluginDir : pluginDirs) {
                    List<URL> urls = Arrays.stream(pluginDir.listFiles()).map(File::toURI).map(this::uri2Url).collect(Collectors.toList());
                    pluginsLibs.add(urls);
                }
            }
        }
        return pluginsLibs;
    }

    private URL uri2Url(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

/*
    private def getPluginsLibs() = {
        val dapengPluginPath = System.getProperty(DapengProperties.DAPENG_PLUGIN_PATH)
        if (dapengPluginPath != null && !dapengPluginPath.isEmpty) {
            val pluginsLibs = new util.ArrayList[util.List[URL]]()
            val pluginParentDir = new File(dapengPluginPath)
            if (!pluginParentDir.exists()) {
                throw new FileNotFoundException(s"文件目录不存在: ${dapengPluginPath}")
            }

            val pluginDirs = pluginParentDir.listFiles()
            if (pluginDirs != null) {
                for (pluginDir <- pluginDirs) {
                    val urls = pluginDir.listFiles().toList.map(i => i.asURL)
                    pluginsLibs.add(urls.asJava)
                }
            }
            pluginsLibs
        } else {
            new util.ArrayList[util.List[URL]]()
        }
    }
    */

    private boolean removeServiceProjectArtifact(Iterator<URL> iterator, URL url) {
        String regex = project.getArtifact().getFile().getAbsolutePath().replaceAll("\\\\", "/");

        if ("\\".equals(File.separator)) {
            regex = regex.replace(File.separator, File.separator + File.separator);
        }

        if (url.getFile().matches("^.*" + regex + ".*$")) {
            iterator.remove();

            return true;
        }
        return false;
    }

    private boolean removeTwitterAndScalaDependency(Iterator<URL> iterator, URL url) {
        if (url.getFile().matches("^.*/twitter.*\\.jar$")) {
            iterator.remove();
            return true;
        }
        return false;
    }

    private boolean removeContainerAndBootstrap(Iterator<URL> iterator, URL url) {
        if (url.getFile().matches("^.*/dapeng-container-api.*\\.jar$")) {
            iterator.remove();

            return true;
        }

        if (url.getFile().matches("^.*/dapeng-container-impl.*\\.jar$")) {
            iterator.remove();

            return true;
        }

        if (url.getFile().matches("^.*/dapeng-bootstrap.*\\.jar$")) {
            iterator.remove();

            return true;
        }
        return false;
    }

}
