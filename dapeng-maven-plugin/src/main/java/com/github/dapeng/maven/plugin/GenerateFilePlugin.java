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
                    "-out", targetFilePath});
//            File commonFile = new File(projectPath + "src/main/java/com/github/dapeng/soa/common");
//            if (commonFile.exists()) {
//                deleteDir(commonFile);
//            }
        }
        if ("both".equals(language) || "scala".equals(language)) {
            Scrooge.main(new String[]{"-gen", "scala", "-all",
                    "-in", sourceFilePath,
                    "-out", targetFilePath});

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
