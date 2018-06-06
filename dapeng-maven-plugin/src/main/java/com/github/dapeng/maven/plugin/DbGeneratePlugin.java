package com.github.dapeng.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import scala.Option;
import scala.collection.mutable.MutableList;

import java.io.File;
import java.sql.Connection;

import static com.github.dapeng.maven.plugin.util.DbGeneratorUtil.*;

@Mojo(name = "dbGen", threadSafe = true)
public class DbGeneratePlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String separator = System.getProperty("file.separator");
        String baseTargetPath =new File(project.getBuild().getOutputDirectory()).getAbsolutePath().replace("target" + System.getProperty("file.separator") + "classes", "");
        loadSystemProperties(new File(baseTargetPath +separator + "src" + separator + "main" +separator + "resources" + separator + "db.properties"));

        String db = System.getProperty("db.name");
        String packageName =System.getProperty("package.name");
        String tableName = System.getProperty("table.name");

        Option<Connection> connection = connectJdbc();
        if (connection.isDefined()){
            if (!tableName.isEmpty()){
                System.out.println(" Found Specific tableName:"+tableName+", start to generateDbEntity..");
                generateDbClass(tableName,db,connection.get(),packageName,baseTargetPath);

            }else {
                System.out.println(" No specific tableName found. will generate "+ db +" all tables..");
                MutableList<String> tableNames = getTableNamesByDb(db, connection.get());
                for(int i = 0;i<tableNames.length();i++){
                    Option<String> table = tableNames.get(i);
                    System.out.println(" start to generated "+db+"."+table+" entity file...");
                    generateDbClass(table.get(),db,connection.get(),packageName,baseTargetPath);
                }

            }
        }else{
            System.out.println(" Failed to connect mysql....please check your config in dapeng.properties file...");
        }

    }
}
