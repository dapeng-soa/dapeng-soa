### dapeng工具介紹

#### 功能說明

1. 通过服务名获取运行信息
2. 通过服务名和版本号，获取元信息
3. 通过json文件，请求对应服务，并打印json格式的结果
4. 通过xml文件，请求对应服务，并打印xml格式的结果
5. 通过系统参数，json文件，调用指定服务器的服务并打印结果
6. 通过系统参数，xml文件，调用指定服务器的服务并打印结果
7. 通过服务名/版本号/方法名，获取请求json的示例
8. 通过服务名/版本号/方法名，获取请求xml的示例
9. 获取当前zookeeper中的服务路由信息
10. 指定配置文件，设置路由信息
11. 指定配置文件，反转映射scala实体,thrift结构体，thrift枚举

#### 使用示例

1. 通过服务名获取运行信息

    命令：
    `java -jar dapeng.jar runningInfo com.github.dapeng.soa.hello.service.HelloService`
   
   打印结果：
    ```
   com.github.dapeng.soa.hello.service.HelloService    1.0.0   192.168.0.1 9090
   com.github.dapeng.soa.hello.service.HelloService    1.0.0   192.168.0.1 9091
   com.github.dapeng.soa.hello.service.HelloService    1.0.1   192.168.0.2 9090
    ```
   
   命令： 
   `java -jar dapeng.jar runningInfo com.github.dapeng.soa.hello.service.HelloService 1.0.1`
   
   打印结果：
   ```
   com.github.dapeng.soa.hello.service.HelloService    1.0.1   192.168.0.2 9090
   ```
   
2. 通过服务名和版本号，获取元信息

   命令：`java -jar dapeng.jar metadata com.github.dapeng.soa.hello.service.HelloService 1.0.1`
   
   打印结果：
   ```
   <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
   <service namespace="com.github.dapeng.soa.hello.service" name="HelloService">
       <doc>
    Hello Service
   </doc>
       <meta>
           <version>1.0.0</version>
           <timeout>30000</timeout>
       </meta>
       <methods>
           <method name="sayHello">
               <doc>
               ...
    ```
    
3. 通过json文件，请求对应服务，并打印结果

    命令：`java -jar dapeng.jar request request.json`
    
    request.json
    ```
    {
      "serviceName": "com.github.dapeng.soa.hello.service.HelloService",
      "version": "1.0.0",
      "methodName": "sayHello",
      "params": {
        "name":"Tom"
      }
    }
    ```
    
4. 通过xml文件，请求对应服务，并打印XML格式的结果

    命令：`java -jar dapeng.jar request request.xml`
    
    request.xml
    ```
    <?xml version="1.0" encoding="UTF-8"?>
    <service>
    	<version>1.0.0</version>
    	<methodName>sayHello</methodName>
    	<serviceName>com.github.dapeng.soa.hello.service.HelloService</serviceName>
    	<params>
    	   "name":"Tom"
    	</params>
    </service>
    ```   
    
5. 通过系统参数，json文件，调用指定服务器的服务并打印结果

    命令：`java -Dsoa.service.ip=192.168.0.1 -Dsoa.service.port=9091 -jar dapeng.jar request request.json`
    
    以上命令会调用运行在192.168.0.1：9091的服务

6. 通过系统参数，xml文件，调用指定服务器的服务并打印结果

    命令：`java -Dsoa.service.ip=192.168.0.1 -Dsoa.service.port=9091 -jar dapeng.jar request request.xml`
    
    以上命令会调用运行在192.168.0.1：9091的服务
    
7. 通过服务名/版本号/方法名，获取请求json的示例

    命令：`java -jar dapeng.jar json com.github.dapeng.soa.hello.service.HelloService 1.0.0 sayHello`
    
    打印结果：
    ```
    {
      "serviceName": "com.github.dapeng.soa.hello.service.HelloService",
      "version": "1.0.0",
      "methodName": "sayHello",
      "params": {
        "name":"demoString"
      }
    }
    ```
    
8. 通过服务名/版本号/方法名，获取请求xml的示例

    命令：`java -jar dapeng.jar json com.github.dapeng.soa.hello.service.HelloService 1.0.0 sayHello`
    
    打印结果：
    ```
    {
      "serviceName": "com.github.dapeng.soa.hello.service.HelloService",
      "version": "1.0.0",
      "methodName": "sayHello",
      "params": {
        "name":"demoString"
      }
    }
    ```
9. 获取当前zookeeper中的服务路由信息: `java -jar dapeng.jar routInfo`
   
10. 指定配置文件，设置路由信息: `java -jar dapeng.jar routInfo route.cfg`
   
11. 指定配置文件，scala和thrift实体，枚举映射

    命令：`java -jar dapeng.jar reverse:[po|struct|enum|enumFmt|all|conf] [reverse.conf]`
    
    * 获取样例配置文件，生成在桌面 ~\Desktop\dapeng-reverse-conf\reverse.conf
    
      `java -jar dapeng.jar reverse:conf`
    
    * 获取枚举类生成规则和示例
        
      `java -jar dapeng.jar reverse:enumFmt`
        
    * 根据配置  生成scala实体
        
        `java -jar dapeng.jar reverse:po reverse.conf`
    
    * 根据配置  生成thrift结构体
    
        `java -jar dapeng.jar reverse:struct reverse.conf`
    
    * 根据配置  生成thrift枚举类
    
        `java -jar dapeng.jar reverse:enum reverse.conf`
    
    * 根据配置  生成所有
    
        `java -jar dapeng.jar reverse:all reverse.conf`
    
    reverse.conf 說明:
    
    * 数据库驱动
    
        `dataBaseDriver=com.mysql.jdbc.Driver`
    
    * 数据库连接信息
        ```
        url=jdbc:mysql://localhost:3306/@module?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull
        username=test
        password=123456
        ```
    
    * 生成结构体的时候，中间的包名
        ```
        package = test
        ```
        
    * 将要访问的db
        ```
        db = test
        ```
    * scanAll：反射整个库,  specify: 反转指定表
        ```
        mode = scanAll
        ```
    * specify模式下， 反转列表
        ```
        tables = table1,table2
        ```
