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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Soa Abstract Mojo
 *
 * @author craneding
 * @date 16/1/27
 */
public abstract class SoaAbstractMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    protected ClassLoader getClassLoader() throws MojoExecutionException {
        URLClassLoader urlClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        URL[] urLs = urlClassLoader.getURLs();

        List<URL> classpathURLs = new ArrayList<URL>(Arrays.asList(urLs));

        //this.addRelevantPluginDependenciesToClasspath( classpathURLs );
        this.addRelevantProjectDependenciesToClasspath(classpathURLs);
        //this.addAdditionalClasspathElements( classpathURLs );
        return new URLClassLoader(classpathURLs.toArray(new URL[classpathURLs.size()]));
    }

    protected void addRelevantProjectDependenciesToClasspath(List<URL> path) throws MojoExecutionException {
        try {
            getLog().debug("Project Dependencies will be included.");

            List<Artifact> artifacts = new ArrayList<Artifact>();
            List<File> theClasspathFiles = new ArrayList<File>();

            collectProjectArtifactsAndClasspath(artifacts, theClasspathFiles);

            for (File classpathFile : theClasspathFiles) {
                URL url = classpathFile.toURI().toURL();
                getLog().debug("Adding to classpath : " + url);
                path.add(url);
            }

            for (Artifact classPathElement : artifacts) {
                getLog().debug("Adding project dependency artifact: " + classPathElement.getArtifactId()
                        + " to classpath");
                path.add(classPathElement.getFile().toURI().toURL());
            }

        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error during setting up classpath", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void collectProjectArtifactsAndClasspath(List<Artifact> artifacts, List<File> theClasspathFiles) {
        artifacts.addAll(project.getRuntimeArtifacts());
        artifacts.addAll(project.getSystemArtifacts());
        theClasspathFiles.add(new File(project.getBuild().getOutputDirectory()));
    }

    protected void joinNonDaemonThreads(ThreadGroup threadGroup) {
        boolean foundNonDaemon;
        do {
            foundNonDaemon = false;
            Collection<Thread> threads = getActiveThreads(threadGroup);
            for (Thread thread : threads) {
                if (thread.isDaemon()) {
                    continue;
                }
                foundNonDaemon = true; // try again; maybe more threads were created while we were busy
                joinThread(thread, 0);
            }
        }
        while (foundNonDaemon);
    }

    protected Collection<Thread> getActiveThreads(ThreadGroup threadGroup) {
        Thread[] threads = new Thread[threadGroup.activeCount()];
        int numThreads = threadGroup.enumerate(threads);
        Collection<Thread> result = new ArrayList<Thread>(numThreads);
        for (int i = 0; i < threads.length && threads[i] != null; i++) {
            result.add(threads[i]);
        }
        return result; // note: result should be modifiable
    }

    protected void joinThread(Thread thread, long timeoutMsecs) {
        try {
            getLog().debug("joining on thread " + thread);
            thread.join(timeoutMsecs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // good practice if don't throw
            getLog().warn("interrupted while joining against thread " + thread, e); // not expected!
        }
        if (thread.isAlive()) // generally abnormal
        {
            getLog().warn("thread " + thread + " was interrupted but is still alive after waiting at least " + timeoutMsecs + "msecs");
        }
    }

    /**
     * a ThreadGroup to isolate execution and collect exceptions.
     */
    class IsolatedThreadGroup extends ThreadGroup {
        private Throwable uncaughtException; // synchronize access to this

        public IsolatedThreadGroup(String name) {
            super(name);
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            if (throwable instanceof ThreadDeath) {
                return; // harmless
            }
            synchronized (this) {
                if (uncaughtException == null) // only remember the first one
                {
                    uncaughtException = throwable; // will be reported eventually
                }
            }
            getLog().warn(throwable);
        }
    }

}
