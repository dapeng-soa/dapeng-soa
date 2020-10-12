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

import com.github.dapeng.code.Scrooge;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Created by jackliang on 2017/10/19.
 */
@Mojo(name = "thriftGenerator")
public class GenerateFilePlugin extends AbstractMojo {

    @Parameter(property = "thriftGenerator.sourceFilePath")
    private String sourceFilePath;

    @Parameter(property = "thriftGenerator.targetFilePath")
    private String targetFilePath;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * 1、java  2、scala 3、both
     */
    @Parameter(property = "thriftGenerator.language", defaultValue = "both")
    private String language;

    /**
     * delete old file
     */
    @Parameter(property = "thriftGenerator.isDelete", defaultValue = "true")
    private String isDelete;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String separator = System.getProperty("file.separator");
        String projectPath = new File(project.getBuild().getOutputDirectory()).getAbsolutePath().replace("target" + System.getProperty("file.separator") + "classes", "");
        sourceFilePath = projectPath + (sourceFilePath == null ? "src" + separator + "main" + separator + "resources" + separator + "thrift" + separator : sourceFilePath);
        targetFilePath = projectPath + (targetFilePath == null ? "src" + separator + "main" + separator : targetFilePath);

        System.out.println(" sourceFilePath: " + sourceFilePath);
        System.out.println(" targetFilePath: " + targetFilePath);

        if ("both".equals(language) || "java".equals(language)) {
            Scrooge.main(new String[]{"-gen", "java", "-all",
                    "-in", sourceFilePath,
                    "-out", targetFilePath,(isDelete.equals("true")?"":"-notDel" ),
                    "-groupId",project.getGroupId(),
                    "-artifactId",project.getArtifactId(),
                    "-modelVersion",project.getModelVersion()
            });
//            File commonFile = new File(projectPath + "src/main/java/com/github/dapeng/soa/common");
//            if (commonFile.exists()) {
//                deleteDir(commonFile);
//            }
        }
        if ("both".equals(language) || "scala".equals(language)) {
            Scrooge.main(new String[]{"-gen", "scala", "-all",
                    "-in", sourceFilePath,
                    "-out", targetFilePath,(isDelete.equals("true")?"":"-notDel" ),
                    "-groupId",project.getGroupId(),
                    "-artifactId",project.getArtifactId(),
                    "-modelVersion",project.getModelVersion()
            });

//            File scalaCommonFile = new File(projectPath + "src/main/scala/com/github/dapeng/soa/scala/common");
//            if (scalaCommonFile.exists()) {
//                deleteDir(scalaCommonFile);
//            }
        }
    }

    private static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                if (!"serializer".equals(children[i])) {
                    deleteDir(new File(dir, children[i]));

                }
            }
        }
        if (!"serializer".equals(dir.getName())) {
            dir.delete();
        }

    }

}
