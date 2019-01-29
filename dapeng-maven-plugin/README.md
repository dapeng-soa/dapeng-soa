#### Maven集成Soa Plugin
修改maven的主配置文件（${MAVEN_HOME}/conf/settings.xml文件或者 ~/.m2/settings.xml文件），添加如下配置：

```
<pluginGroups>
    <pluginGroup>com.github.dapeng</pluginGroup>
  </pluginGroups>
```

#### Maven运行

```
mvn compile com.github.dapeng:dapeng-maven-plugin:2.1.1:run
```