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
|   |   |-- service-e/classes              
|   |-- logs                               日志目录
-------------------------------------------------------
```

### 工程目录说明

```
|-- dapeng
|   |-- dapeng-api-doc                 服务api站点工程
|   |-- dapeng-bootstrap               启动模块工程
|   |-- dapeng-code-generator          服务idl代码生成工程
|   |-- dapeng-container               容器工程
|   |-- dapeng-core                    核心工程
|   |-- dapeng-maven-plugin            Maven开发插件工程
|   |-- dapeng-monitor
|   |   |-- dapeng-monitor-api         监控模块api工程
|   |   |-- dapeng-monitor-druid       druid的监控工具
|   |   |-- dapeng-monitor-influxdb    监控模块api实现工程(influxdb版本)
|   |-- dapeng-registry
|   |   |-- dapeng-registry-api        注册模块api工程
|   |   |-- dapeng-registry-zookeeper  注册模块api实现工程(zookeeper版本)
|   |-- dapeng-remoting
|   |   |-- dapeng-remoting-api        客户端通讯模块api工程
|   |   |-- dapeng-remoting-netty      客户端通讯模块api实现工程(netty版本)
|   |   |-- dapeng-remoting-socket     客户端通讯模块api实现工程(socket版本)
|   |-- dapeng-spring                  spring扩展模块工程
```

### 服务开发简易说明

#### 安装soa项目到本地maven仓库

```
mvn clean install
```

#### 例子工程

```
git clone https://github.com/dapeng-soa/dapeng-soa-hello.git
```

#### thrift idl 定义服务接口

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

#### 服务接口代码生成：

> 打包服务接口代码工程(`dapeng-code-generator`): `mvn clean package`
>
> 输出的可执行jar包目录: `dapeng-code-generator/target/dapeng-code-generator-2.0.0-jar-with-dependencies.jar`

打印帮助命令

```
java -jar dapeng-code-generator-2.0.0-jar-with-dependencies.jar

-----------------------------------------------------------------------
 args: -gen metadata,js,json file
 Scrooge [options] file
 Options:
   -out dir    Set the output location for generated files.
   -gen STR    Generate code with a dynamically-registered generator.
               STR has the form language[val1,val2,val3].
               Keys and values are options passed to the generator.
   -v version  Set the version of the Service generated.
   -in dir     Set input location of all Thrift files.
   -all        Generate all structs and enums

 Available generators (and options):
   metadata
   js
   json
   java
-----------------------------------------------------------------------
```

生成thrift idl 定义服务接口代码

```
java -jar dapeng-code-generator-2.0.0-jar-with-dependencies.jar -gen java -out F:\hello F:\hello\hello_domain.thrift,F:\hello\hello_service.thrift

# 说明：
# 1. `-gen java` 表示生成java代码； 
# 2. `-out F:\hello`表示生成代码到`F:\hello`文件夹；
# 3. `-in /home/thrift/`表示thrift文件所在文件夹，可以省略一个个声明文件地址
# 4. 多个thrift文件使用`,`分隔； 
# 5. 生成的xml文件在`F:\hello`文件夹，java类在`F:\hello\java-gen`文件夹。
```

#### 创建API工程

`hello-api`工程会被服务端和客户端依赖。

新建maven工程，即`hello-api`工程，依赖于`dapeng-remoting-api`:
```
<dependency>
    <groupId>com.github.dapeng</groupId>
    <artifactId>dapeng-remoting-api</artifactId>
    <version>2.0.0</version>
</dependency>
```

将上一步中生成的java代码，拷贝入对应的package。将上一步中生成的xml文件，拷贝入`resources`文件夹。示例如图：

![api结构示例](http://7xnl6z.com1.z0.glb.clouddn.com/com.github.dapeng.soaapi_sturct_demo.png)

最后`mvn clean install`此项目。

#### 创建Service工程

`hello-service`工程依赖于`hello-api`工程和`dapeng-spring`包，在工程中实现api中的接口类，在方法中实现具体的业务逻辑。

* 依赖：

```
<dependency>
    <groupId>com.github.dapeng</groupId>
    <artifactId>hello-api</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.github.dapeng</groupId>
    <artifactId>dapeng-spring</artifactId>
    <version>2.0.0</version>
</dependency>
```

* 实现类：

```
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) throws SoaException {
        return "hello, " + name;
    }

    @Override
    public String sayHello2(Hello hello) throws SoaException {
        if (hello.getName().equals("bad")) {
            throw new SoaException("hello-001", "so bad");
        } else {
            String message;
            if (!hello.getMessage().isPresent())
                message = "you message is emtpy";
            else
                message = "you message is '" + hello.getMessage().get() + "'";

            return "hello, " + hello.getName() + ", " + message;
        }
    }
}
```

* 声明服务，使得容器启动时加载和注册该服务：

在`resources/META-INF/spring/`文件夹下新建services.xml

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xmlns:soa="http://soa-springtag.dapeng.com/schema/service"
   xsi:schemaLocation="http://www.springframework.org/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd
    http://soa-springtag.dapeng.com/schema/service
    http://soa-springtag.dapeng.com/schema/service/service.xsd">

    <bean id="helloService" class="com.github.dapeng.soa.hello.HelloServiceImpl"/>
    <soa:service ref="helloService"/>
</beans>
```
    
#### 开发模式启动服务

> 前提:需要把`dapeng-maven-plugin`安装到本地maven仓库

##### 安装Maven插件

安装`dapeng-maven-plugin`工程

* 源码手动安装

```
cd dapeng/dapeng-maven-plugin
mvn clean install
```


##### Maven启动服务容器

> 启动服务容器在开发模式下可以选择`本地模式`或`远程模式`
>
> 可在无开发ide环境下或在ide开发环境下运行
> 
> 可以使用`dapeng`的插件简称,需要在本地maven进行配置

* dapeng插件简称配置(不使用不用配置)

修改maven的主配置文件（${MAVEN_HOME}/conf/settings.xml文件或者 ~/.m2/settings.xml文件）

```
<pluginGroups>
    <pluginGroup>com.github.dapeng</pluginGroup>
  </pluginGroups>
```

* 本地模式(无需启动zookeeper)

> 默认启动端口:9090

启动命令:
```
# 第一种(简称)
cd hello-service
mvn compile dapeng:run -Dsoa.remoting.mode=local

# 第二种
cd hello-service
mvn compile com.github.dapeng:dapeng-maven-plugin:2.0.0:run -Dsoa.remoting.mode=local
```

* 远程模式(需要启动zookeeper)

> 默认启动端口:9090

启动命令:
```
# 第一种(简称)
cd hello-service
mvn compile dapeng:run

# 第二种
cd hello-service
mvn compile com.github.dapeng:dapeng-maven-plugin:2.0.0:run
```

* 启动可选参数

```
# -Dsoa.zookeeper.host=127.0.0.1:2181
# -Dsoa.container.port=9090
# -Dsoa.monitor.enable=false
```

#### 客户端调用服务

##### 依赖配置

客户端要依赖`hello-api`,`dapeng-registry-zookeeper`和`dapeng-remoting-netty`
```
<dependency>
    <groupId>com.github.dapeng</groupId>
    <artifactId>hello-api</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.github.dapeng</groupId>
    <artifactId>dapeng-registry-zookeeper</artifactId>
    <version>2.0.0</version>
</dependency>
<dependency>
    <groupId>com.github.dapeng</groupId>
    <artifactId>dapeng-remoting-netty</artifactId>
    <version>2.0.0</version>
</dependency>
```


##### 本地模式

> 非本地模式不用配置

启动参数:

```
# -Dsoa.remoting.mode=local
```

可选参数:

```
# -Dsoa.service.port=9091
```

##### 远程模式

> 无必选参数

可选参数:

```
# -Dsoa.zookeeper.host=127.0.0.1:2181
# -Dsoa.service.port=9091
```

##### 调用服务测试

测试代码：

```
HelloServiceClient client = new HelloServiceClient();
System.out.println(client.sayHello("LiLei"));
```

#### 文档站点和在线测试

* 启动服务后，在浏览器访问地址：[http://localhost:8080/index.htm](http://localhost:8080/index.htm),点击`api`标签，即可看到当前运行的服务信息：

![API站点页面](http://7xnl6z.com1.z0.glb.clouddn.com/com.github.dapeng.soa1.png)
    
* 点击对应服务，可以查看该服务相关信息，包括全名称、版本号、方法列表、结构体和枚举类型列表等，点击对应项目可查看详情。
* 从方法详情页面点击在线测试，进入在线测试页面：

![在线测试页面](http://7xnl6z.com1.z0.glb.clouddn.com/com.github.dapeng.soa2.png)

* 输入必填项参数，点击提交请求，即可请求本机当前运行的服务，并获得返回数据：

![请求数据](http://7xnl6z.com1.z0.glb.clouddn.com/com.github.dapeng.soa.api_test_req.png)

![返回数据](http://7xnl6z.com1.z0.glb.clouddn.com/com.github.dapeng.soa.api_test_rsp.png)

* 控制台可以看到相应的请求信息：

其中trans-pool-1-thread-2是后台服务打印日志

![控制台打印信息](http://7xnl6z.com1.z0.glb.clouddn.com/com.github.dapeng.soa4.png)


#### 分布式事务

为了解决分布式框架中，跨服务跨库调用过程的数据一致性问题，框架提供了一个分布式事务管理的解决方案。

##### 原理

在设计服务时，声明该服务方法是由全局事务管理器管理，该方法中调用的其他服务方法，可以声明为一个事务过程。当调用一个全局事务方法时，容器将为该次调用生成唯一id，自动记录该事务，以及该全局
事务下的所有子事务过程，并记录状态。由一个定时事务管理器定时扫描记录，按照约定向前或者回滚一个失败的全局事务中已成功且未回滚(向前)的子事务过程。

##### 使用

1. 在IDL中声明某方法是一个全局事务过程

    ```
    service AuctionService {
    
        /**
        * @SoaGlobalTransactional
        **/
        auction_domain.TBidAuctionResponse bidAuction(1: auction_domain.TBidAuctionCondition bidAuctionCondition)
    
    }
    ```
    即在该方法注释中，添加"@SoaGlobalTransactional"字符串。
    
2. 在IDL中声明某方法是一个子事务过程,并声明对应的回滚方法(方法名_rollback)

    ```
    service AccountService {
    
          /**
            * @IsSoaTransactionProcess
            **/
           account_domain.TAccountJournal freezeBalance( 1:account_domain.TFreezeBalanceRequest freezeBalanceRequest),
           
        
           /**
            * freezeBalance接口的回调方法
           **/
           account_domain.TAccountJournal freezeBalance_rollback(),
    }
    ```
    在方法注释中使用字符串"@IsSoaTransactionProcess"声明该方法是一个子事务过程，同时也必须定义一个对应的回滚方法。定时事务管理器会自动调用该回滚方法，由开发者自己实现回滚方法。
    
3. 在数据库中添加全局事务表和事务过程表
 
    ```
    CREATE TABLE `global_transactions` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `status` smallint(2) NOT NULL DEFAULT '1' COMMENT '状态，1：新建；2：成功；3：失败；4：已回滚；5：已部分回滚；99：挂起；',
      `curr_sequence` int(11) NOT NULL COMMENT '当前过程序列号',
      `created_at` datetime NOT NULL,
      `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      `created_by` int(11) DEFAULT NULL,
      `updated_by` int(11) DEFAULT NULL,
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8 COMMENT='全局事务表';
    
    CREATE TABLE `global_transaction_process` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `transaction_id` int(11) NOT NULL,
      `transaction_sequence` int(11) NOT NULL COMMENT '过程所属序列号',
      `status` smallint(2) NOT NULL DEFAULT '1' COMMENT '过程当前状态，1：新建；2：成功；3：失败；4：未知，5：已回滚；',
      `expected_status` smallint(6) NOT NULL DEFAULT '1' COMMENT '过程目标状态，1：成功；2：已回滚；',
      `service_name` varchar(128) NOT NULL COMMENT '服务名称',
      `version_name` varchar(32) NOT NULL COMMENT '服务版本',
      `method_name` varchar(32) NOT NULL COMMENT '方法名称',
      `rollback_method_name` varchar(32) NOT NULL COMMENT '回滚方法名称',
      `request_json` text NOT NULL COMMENT '过程请求参数Json序列化',
      `response_json` text NOT NULL COMMENT '过程响应参数Json序列化',
      `redo_times` int(11) DEFAULT '0' COMMENT '重试次数',
      `next_redo_time` datetime DEFAULT NULL COMMENT '下次重试时间',
      `created_at` datetime NOT NULL,
      `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      `created_by` int(11) DEFAULT NULL,
      `updated_by` int(11) DEFAULT NULL,
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8 COMMENT='事务过程表';
    ```
    
    

<h4 id="thrift"> Thrift IDL 补充说明</h4>

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


##### 忽略日志

* 框架默认打印所有请求内容和返回内容，若不想打印某字段，可以在struct描述时，在该字段注释中添加`@logger(level="off")`字符串，例：

    ```
    /**
     * @logger(level="off") 
     * 文章内容,不打印
     */
    5: optional string content,
    ```
    
##### 异步方法

* 框架支持客户端和服务端异步调用。只需要在IDL声明方法时，注释中添加`@SoaAsyncFunction`字符串，则可以自动生成异步客户端和服务端代码，例：
    
    ```
    service HelloService {
    
        /**
        * @SoaAsyncFunction
        **/
        string sayHello(1:string name)
    }
    ```

##### 分布式事务

1. 在IDL中声明某方法是一个全局事务过程

    ```
    service AuctionService {
    
        /**
        * @SoaGlobalTransactional
        **/
        auction_domain.TBidAuctionResponse bidAuction(1: auction_domain.TBidAuctionCondition bidAuctionCondition)
    
    }
    ```
    即在该方法注释中，添加"@SoaGlobalTransactional"字符串。
    
2. 在IDL中声明某方法是一个子事务过程,并声明对应的回滚方法(方法名_rollback)

    ```
    service AccountService {
    
          /**
            * @IsSoaTransactionProcess
            **/
           account_domain.TAccountJournal freezeBalance( 1:account_domain.TFreezeBalanceRequest freezeBalanceRequest),
           
        
           /**
            * freezeBalance接口的回调方法
           **/
           account_domain.TAccountJournal freezeBalance_rollback(),
    }
    ```
    在方法注释中使用字符串"@IsSoaTransactionProcess"声明该方法是一个子事务过程，同时也必须定义一个对应的回滚方法。定时事务管理器会自动调用该回滚方法，由开发者自己实现回滚方法。
