[TOC]

### 容器部署

#### 运行脚本

```
cd dapeng-container

sh dev.sh
```

#### 输出目录

```
dapeng-container/target/dapeng-container
```

#### 目录说明

```
|-- dapeng-container
|   |-- bin                                
|   |   |-- lib                            平台jar包目录
|   |   |   |-- dapeng-container.jar
|   |   |   |-- ...                   
|   |   |-- startup.sh                     
|   |   |-- shutdown.sh                    
|   |   |-- dapeng-bootstrap.jar
|   |-- lib                                公共依赖jar包目录
|   |   |-- dapeng-core.jar
|   |   |-- ...
|   |-- conf                               配置文件目录
|   |   |-- server-conf.xml                
|   |   |-- logback.xml                    
|   |-- apps                               服务目录
|   |   |-- service-a/*.jar                
|   |   |-- service-b.jar                  
|   |   |-- service-c_d_f.jar              
|   |   |-- service-e/jars              
|   |-- logs                               日志目录
-------------------------------------------------------
```

### 工程目录说明

```
|-- dapeng
|   |-- dapeng-api-doc                 服务api站点工程
|   |-- dapeng-code-generator          服务idl代码生成工程
|   |-- dapeng-container               容器工程
|   |   |-- dapeng-bootstrap           启动模块工程
|   |   |-- dapeng-container-api       dapeng主容器Api
|   |   |-- dapeng-container-impl      dapeng主容器实现模块
|   |-- dapeng-core                    dapeng核心工程
|   |-- dapeng-client-netty            dapeng客户端通讯模块(netty)
|   |-- dapeng-message                 dapeng消息订阅发布模块
|   |-- dapeng-spring                  spring扩展模块工程
|   |-- dapeng-registry
|   |   |-- dapeng-registry-zookeeper  注册模块api实现工程(zookeeper版本)
|   |-- dapeng-maven-plugin            Maven开发插件工程
|   |-- dapeng-monitor
|   |   |-- dapeng-monitor-api         监控模块api工程
|   |   |-- dapeng-monitor-druid       druid的监控工具
|   |   |-- dapeng-monitor-influxdb    监控模块api实现工程(influxdb版本)
|   |-- dapeng-counter                 大鹏influxdb计数模块
|   |-- dapeng-json                    流式处理json模式
```

### 服务开发简易说明
环境需求:

> 
* jdk8 或以上版本
* scala 2.12.* 或以上
* zookeeper
* maven or sbt 

#### 1. 安装dapeng-soa项目到本地maven仓库

```
mvn clean install
```

#### 2. 新建maven项目 - demo (java版本)

##### 2.1 修改pom.xml配置
```
<properties>
	<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
	<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
	<java.version>1.8</java.version>
	<maven.compiler.source>1.8</maven.compiler.source>
	<maven.compiler.target>1.8</maven.compiler.target>
	<dapeng.version>2.0.5</dapeng.version> <!-- 定义dapeng框架版本 -->
</properties>

<dependencies>
	<dependency>
		<groupId>com.github.dapeng</groupId>
		<artifactId>dapeng-client-netty</artifactId>
		<version>${dapeng.version}</version>
	</dependency>
	<dependency>
    	<groupId>com.github.dapeng</groupId>
    	<artifactId>dapeng-spring</artifactId>
    	<version>${dapeng.version}</version>
	</dependency>
</dependencies>

<!-- 基于thrift生成源码插件 -->
<build>
	<plugins>
		<plugin>
			<groupId>com.github.dapeng</groupId>
			<artifactId>dapeng-maven-plugin</artifactId>
			<version>${dapeng.version}</version>
			<executions>
				<execution>
					<phase>generate-sources</phase>
					<goals>
						<goal>thriftGenerator</goal>
					</goals>
					<configuration>
						<!--配置生成哪种语言的代码[java,scala]-->
						<language>java</language>
					    <!-- thrift文件存放路径 -->
					    <sourceFilePath>src/main/resources/thrifts/</sourceFilePath>
					    <!-- 生成的源码存放路径 -->
						<targetFilePath>src/main/</targetFilePath>
					</configuration>
				</execution>
			</executions>
		</plugin>
	</plugins>
</build>
```

##### 2.2 thrift idl 定义服务接口

* hello_domain.thrift:

```
namespace java com.github.dapeng.soa.hello.domain

struct Hello {

    1: string name,

    2: optional string message

}
```

* hello_service.thrift：

```
include "hello_domain.thrift"

namespace java com.github.dapeng.soa.hello.service

/**
* Hello Service
**/
service HelloService {

    /**
    * say hello
    **/
    string sayHello(1:string name),

    string sayHello2(1:hello_domain.Hello hello)
    
}
```

> [thrift idl补充说明](#thrift)

##### 2.3 服务接口代码生成：

```mvn clean package```
生成的文件: 
![](https://github.com/dapeng-soa/documents/blob/master/images/dapeng-thrift/demo.png?raw=true)

##### 2.4 服务实现 HelloServiceImpl
```
package com.github.dapeng.demo.hello.impl;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.demo.hello.domain.Hello;
import com.github.dapeng.demo.hello.service.HelloService;

public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) throws SoaException {
        return name;
    }

    @Override
    public String sayHello2(Hello hello) throws SoaException {
        return hello.toString();
    }
}

```
##### 2.5 声明服务，使得容器启动时加载和注册该服务：

> 在`resources/META-INF/spring/`文件夹下新建services.xml

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:soa="http://soa-springtag.dapeng.com/schema/service"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd
    http://soa-springtag.dapeng.com/schema/service
    http://soa-springtag.dapeng.com/schema/service/service.xsd">

    <bean id="helloService" class="com.github.dapeng.demo.hello.impl.HelloServiceImpl"/>
    <soa:service ref="helloService"/>
</beans>
```
    
##### 2.6 开发模式启动服务

> 前提:需要把`dapeng-maven-plugin`安装到本地maven仓库

###### 2.6.1 安装Maven插件

安装`dapeng-maven-plugin`工程

* 源码手动安装

```
cd dapeng-soa/dapeng-maven-plugin
mvn clean install
```


##### Maven启动服务容器

> 可在无开发ide环境下或在ide开发环境下运行
> 
> 可以使用`dapeng`的插件简称,需要在本地maven进行配置

* dapeng插件简称配置(不使用简称可以不用配置)

修改maven的主配置文件（${MAVEN_HOME}/conf/settings.xml文件或者 ~/.m2/settings.xml文件）

```
<pluginGroups>
    <pluginGroup>com.github.dapeng</pluginGroup>
  </pluginGroups>
```

启动命令:
> 可选参数:
 -Dsoa.zookeeper.host=127.0.0.1:2181 
 -Dsoa.container.port=9090   -- 大鹏容器端口, 默认:9090
 -Dsoa.monitor.enable=false
 -Dsoa.apidoc.port=8080       --大鹏文档站点端口, 默认: 8080
 
```
# 第一种(简称,配置了pluginGroup后可用)
cd hello-service
mvn compile dapeng:run

# 第二种(不需配置maven pluginGroup)
cd hello-service
mvn compile com.github.dapeng:dapeng-maven-plugin:2.0.5:run

# 指定参数运行
cd hello-service
mvn compile com.github.dapeng:dapeng-maven-plugin:2.0.5:run -Dsoa.apidoc.port=8089
```

##### 2.7 调用服务测试

###### 2.7.1 本地测试代码：

```
import com.github.dapeng.demo.hello.HelloServiceClient;

public class HelloServiceTest {

    public static void main(String[] args) throws Exception{
        HelloServiceClient helloService = new HelloServiceClient();
        String result = helloService.sayHello("hello world!");
        System.out.println(result);
    }
}
```

##### 2.7.2 文档站点和在线测试
> 服务启动后，输入网址 `localhost:8080`, 如果改了apidoc.port的值，输入你指定的端口即可

![index](https://github.com/dapeng-soa/documents/blob/master/images/dapeng-apiDoc/apidoc_index.png?raw=true)

> 点击api标签, 可以看到我们启动运行的Hello 服务

![index](https://github.com/dapeng-soa/documents/blob/master/images/dapeng-apiDoc/apidoc_service_index.png?raw=true)

> 点击`helloService`, 可以查看服务详情

![index](https://github.com/dapeng-soa/documents/blob/master/images/dapeng-apiDoc/apidoc_service_detail.png?raw=true)

> 点击对应的方法，即可在线测试接口

![index](https://github.com/dapeng-soa/documents/blob/master/images/dapeng-apiDoc/apidoc_method_test.png?raw=true)
![index](https://github.com/dapeng-soa/documents/blob/master/images/dapeng-apiDoc/apidoc_test_response.png?raw=true)


#### 3. 创建Scala版本的Demo项目 (sbt构建版本)
> scala 版本的Demo项目我们提供了一个g8 模板，创建非常简单
- 需要安装好sbt环境, 执行以下命令，并根据提示输入对应的配置

```
> sbt new dapeng-soa/dapeng-soa-v2.g8

[info] Loading settings from idea.sbt ...
[info] Loading global plugins from /yourHomePath/.sbt/1.0/plugins
[info] Set current project to github (in build file:/yourProjectPath/)

this is a template to genreate dapeng api service

name [api]: demo     # 输入项目名，不输入直接回车则使用默认值
version ["0.1-SNAPSHOT"]:  # 输入项目版本号
scalaVersion ["2.12.2"]:   # 输入scala版本，建议2.12以上
organization [com.github.dapeng-soa]: # 输入你的服务groupId
resources [resources]:      #源文件文件夹名， 默认值，可以不改
api [demo-api]:           # 可使用默认值
service [demo-service]:   # 可使用默认值
servicePackage []: com.github.dapeng 
dapengVersion [2.0.5]:    # dapeng框架的版本，现在是2.0.5
#项目源码包路径，需要填写,这里我们设为com.github.dapeng

Template applied in ./demo
```

##### 3.1 编译api

```
> cd demo
> sbt clean api/compile api/package

.......
[success] Total time: 15 s, completed 2018-9-28 14:57:44
Welcome to use generate plugin
Thrift-Generator-Plugin:  No need to regenerate source files. skip..............
[info] Packaging C:\Users\23294\dev\github\demo\demo-api\target\scala-2.12\demo-api_2.12-0.1-SNAPSHOT.jar ...
[info] Done packaging.
[success] Total time: 0 s, completed 2018-9-28 14:57:44
```

> windows 环境的朋友请注意，由于windows系统自带的编码时gbk,在编译的时候需要设置编码为 utf8. 可以新建一个sbt-task, 如下图
![index](https://github.com/dapeng-soa/documents/blob/master/images/dapeng-demo/demo_compile.png?raw=true)

##### 3.2 实现接口
> 我们的g8模板把项目分成了两部分，api & service, 接口文件放到demo-api, 实现则在demo-service, 在demo-service新建 helloServiceImpl (scala版)

src/main/scala/com/github/dapeng/hello/impl/HelloServiceImpl.scala
```
package com.github.dapeng.hello.impl

import com.github.dapeng.hello.scala.domain.Hello
import com.github.dapeng.hello.scala.service.HelloService

class HelloServiceImpl extends HelloService{
  /**
    *
    **
  say hello
    *
    **/
  override def sayHello(name: String): String = name

  /**
    *
    **/
  override def sayHello2(hello: Hello): String = {
    hello.toString
  }
}
```

##### 3.3 修改spring配置
> <b>`demo-service` ->  spring的services.xml配置跟java版本的一致<b/>
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:soa="http://soa-springtag.dapeng.com/schema/service"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://soa-springtag.dapeng.com/schema/service
        http://soa-springtag.dapeng.com/schema/service/service.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">

    <!--参数配置-->
    <context:property-placeholder location="classpath:config_demo.properties" local-override="false"
                                  system-properties-mode="ENVIRONMENT"/>

    <context:component-scan base-package="com.github.dapeng"/>

    <!-- dapeng framework service bean config template -->
    <bean id="helloScalaService" class="com.github.dapeng.hello.impl.HelloServiceImpl"/>
    <soa:service ref="helloScalaService"/>
</beans>
```

##### 3.4 运行服务
> scala 版本我们提供了一个运行插件，只需要运行如下指令即可运行
```
> sbt runContainer

......
09-28 15:21:20 228 dapeng-container-biz-pool-0 INFO [1b944b0195b92ca1] - request[seqId:0]:service[com.github.dapeng.hello.service.HelloService]:version[1.0.0]:method[getServiceMetadata]  
09-28 15:21:20 439 dapeng-container-biz-pool-0 INFO [1b944b0195b92ca1] - response[seqId:0, respCode:0000]:service[com.github.dapeng.hello.service.HelloService]:version[1.0.0]:method[getServiceMetadata] cost:535ms
九月 28, 2018 3:21:20 下午 org.springframework.web.context.ContextLoader initWebApplicationContext
信息: Root WebApplicationContext: initialization completed in 2520 ms
九月 28, 2018 3:21:20 下午 org.springframework.web.servlet.DispatcherServlet initServletBean
信息: FrameworkServlet 'appServlet': initialization started
九月 28, 2018 3:21:20 下午 org.springframework.web.context.support.XmlWebApplicationContext prepareRefresh
信息: Refreshing WebApplicationContext for namespace 'appServlet-servlet': startup date [Fri Sep 28 15:21:20 CST 2018]; parent: Root WebApplicationContext
九月 28, 2018 3:21:20 下午 org.springframework.web.servlet.DispatcherServlet initServletBean
信息: FrameworkServlet 'appServlet': initialization completed in 13 ms
api-doc server started at port: 8192
```

##### 3.5 测试
> <b>测试方式与java版本一致, 请参考 [2.7 调用服务测试]<b/>

#### 4 基于docker模式的发布构建

> 详情请参考该demo工程
https://github.com/dapeng-soa/dapeng-hello

#### 5. Thrift IDL 补充说明

##### Optional类型

* 请求发送和结果返回前，将对实体中所有非Optional类型字段进行校验，若不为Optional类型且为`null`，将直接抛错；

    struct描述时，若字段类型为optional,则在生成java代码时，该字段将会被转为Optional类型，请求发送和结果返回时，不再对该字段做判空校验，例：

    ```
    /**
    * 文章来源
    */
    6: optional string source,
    ```

##### Date类型

1. 在struct描述中，若字段类型为`i64`，且注释中包含`@datatype(name="date")`字符串, 则在java代码生成时，该字段将自动转换为`java.util.Date`类型，例：

    ```
    /**
    * @datatype(name="date")
    **/
    8: i64 createdAt,
    ```

2. 在method描述中，如果参数类型为`i64`，且注释中包含`@datatype(name="date")`字符串，则在java代码生成时，该参数将自动转换为`java.util.Date`类型，例：

    ```
    service HelloService {
    
        /**
        * @datatype(name="date")
        **/
        i64 sayHello(/**@datatype(name="date")**/1:i64 updateAt, /**@datatype(name="bigdecimal")**/2:double amount),
    }
    ```

3. 在method描述中，若返回结果类型为`i64`，且注释中包含`@datatype(name="date")`字符串，则在java代码生成时，该返回结果将自动转换为`java.util.Date`类型，例子同上。

##### BigDecimal类型

1. 在struct描述中，若字段类型为`double`，且注释中包含`@datatype(name="bigdecimal")`字符串, 则在java代码生成时，该字段将自动转换为`java.math.BigDecimal`类型，例：

    ```
    /**
    * @datatype(name="bigdecimal")
    **/
    1: doublle amount,
    ```

2. 在method描述中，如果参数类型为`double`，且注释中包含`@datatype(name="bigdecimal")`字符串，则在java代码生成时，该参数将自动转换为`java.math.BigDecimal`类型，例：

    ```
    service HelloService {
    
        /**
        * @datatype(name="bigdecimal")
        **/
        double sayHello(/**@datatype(name="date")**/1:i64 updateAt, /**@datatype(name="bigdecimal")**/2:double amount),
    }
    ```

3. 在method描述中，若返回结果类型为`double`，且注释中包含`@datatype(name="bigdecimal")`字符串，则在java代码生成时，该返回结果将自动转换为`java.math.Bigdecimal`类型，例子同上。
