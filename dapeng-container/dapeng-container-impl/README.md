### dapeng构建脚本使用说明

> dapeng-container-impl 有三种构建方式
* 1 简单的打jar包
* 2 执行构建dapeng容器启动所需要的资源文件目录
* 3 构建dapeng容器镜像


#### 1. 简单的打jar包 

```
# 使用默认的构建方式，只会构建dapeng-container-impl自身的jar包
> cd dapeng-container-impl
> mvn clean package 

......
[INFO] Building jar: */dapeng-soa/dapeng-container/dapeng-container-impl/target/dapeng-container-impl-2.1.1-SNAPSHOT-sources.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 5.741 s
[INFO] Finished at: 2018-10-10T18:25:07+08:00
[INFO] ------------------------------------------------------------------------

```

#### 2. 执行构建dapeng容器启动所需要的资源文件
构建dapeng容器启动所需目录结构

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
> cd dapeng-container-impl
> sh dev.sh
```


#### 3. 构建dapeng容器镜像
```
> cd dapeng-container-impl
> sh dockerBuild.sh
```
