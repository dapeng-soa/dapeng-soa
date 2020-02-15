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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.net.MalformedURLException;
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

    List<URL> buildRuntimeURLs(){
        ArrayList<URL> results = new ArrayList<>();
        List<Artifact> arts = project.getRuntimeArtifacts();
        for(Artifact art: arts) {
            try {
                results.add(art.getFile().toURL());
            }
            catch(MalformedURLException ex){}
        }

        URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
        for(URL url: urls){
            if(url.toString().endsWith("/target/classes/")){
                results.add(url);
            }
        }

        return results;
    }

    List<URL> buildPlatformURLs(List<URL> rtURLs){
        URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
//        for(URL url: urls) System.out.println("url:" + url);

        ArrayList<URL> results = new ArrayList<>();
        List<Artifact> arts = project.getRuntimeArtifacts();
        for(URL url: urls) {
            boolean contains = false; //rtURLs.contains(url);
            if(!contains) results.add(url);
        }

//        System.out.println("platformURLs size:" + results.size());
//        for(URL url: results){
//            System.out.println("platform:" + url);
//        }
//        System.out.println("end\n\n");
        return results;
    }

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

                List<URL> rtURLs = buildRuntimeURLs();
                List<URL> platformURLs = buildPlatformURLs(rtURLs);

                URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();

                CoreClassLoader coreClassLoader = new CoreClassLoader(platformURLs.toArray(new URL[platformURLs.size()]));
                ContainerClassLoader containerClassLoader = new ContainerClassLoader(new URL[]{}, coreClassLoader);
                ApplicationClassLoader appClassLoader = new ApplicationClassLoader(rtURLs.toArray(new URL[rtURLs.size()]), null, coreClassLoader);

                List<ClassLoader> appClassLoaders = new ArrayList<>();
                appClassLoaders.add(appClassLoader);

                System.out.println("------set classloader-------------");
                Thread.currentThread().setContextClassLoader(appClassLoader);

                Bootstrap.startup(containerClassLoader,appClassLoaders);

            } catch (Exception e) {
                Thread.currentThread().getThreadGroup().uncaughtException(Thread.currentThread(), e);
            }
        }, "RunContainerPlugin" + ".main()");
        bootstrapThread.setContextClassLoader(getClassLoader());
        bootstrapThread.start();

        joinNonDaemonThreads(threadGroup);
    }

}
