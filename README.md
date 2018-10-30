<img width="254" src="https://github.com/dapeng-soa/documents/blob/master/images/dapeng-logo/%E5%A4%A7%E9%B9%8Flogo-03.png" alt="dapeng-soa" title="dapeng-soa"/>

[![Language](https://img.shields.io/badge/language-Java-orange.svg)](https://www.oracle.com)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.dapeng-soa/dapeng-parent/badge.svg)](https://search.maven.org/search?q=com.github.dapeng-soa)
[![GitHub release](https://img.shields.io/github/release/dapeng-soa/dapeng-soa.svg)](https://github.com/dapeng-soa/dapeng-soa/releases)
[![DockerHub](https://img.shields.io/badge/docker-dapengsoa-yellow.svg)](https://hub.docker.com/r/dapengsoa/dapeng-container/)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)


Dapeng-soa 是一个轻量级、高性能的微服务框架，构建在Netty以及定制的精简版Thrift之上。 同时，从Thrift IDL文件自动生成的服务元数据信息是本框架的一个重要特性，很多其它重要特性都依赖于服务元数据信息。 最后，作为一站式的微服务解决方案，Dapeng-soa还提供了一系列的脚手架工具以支持用户快速的搭建微服务系统，例如:
- [x] api网关([dapeng-mesh](https://github.com/dapeng-soa/dapeng-mesh)), 提供基于服务元数据以及流式处理的Json模块用于处理http-json请求跟Thrift协议之间的相互转换。
- [x] 在线文档以及测试站点([dapeng-api-doc](https://github.com/dapeng-soa/dapeng-api-doc))，直接基于服务元数据生成，确保跟代码保持同步。
- [x] 命令行工具([dapeng-cli](https://github.com/dapeng-soa/dapeng-cli))，提供命令行或者脚本的方式跟服务集群交互，可用于服务运行时状态监控、数据修复等。
- [x] 配置部署中心([dapeng-config-server](https://github.com/dapeng-soa/dapeng-config-server))，提供web-gui界面，用于服务配置管理以及服务部署管理。
- [x] maven/sbt插件 for IDEA, 用于在开发过程中快速启动服务容器 
- [x] 项目模板(目前仅支持sbt:g8 template for sbt projects)
- [x] Demo([dapeng-demo](http://demo.dapeng-soa.tech))

# Architecture
<p align="center">
<img src="https://github.com/dapeng-soa/documents/blob/master/images/dapeng-architecture.png" alt="dapeng-soa" title="dapeng-soa"/>
</p>

# Features
- [x] 基于Netty 以及精简版的Thrift 
- [x] 基于Thrift IDL的服务元数据
- [x] 服务注册以及服务自动发现
- [x] 支持Java/Scala客户端代码自动生成
- [x] 支持http-json跟Thrift二进制流的高效相互转换
- [x] 客户端以及服务端全链路同步/异步调用支持
- [x] 多维度智能服务路由以及负载均衡策略，可通过http cookie信息路由([Router](https://github.com/dapeng-soa/dapeng-soa/wiki/Dapeng-Service-Route%EF%BC%88%E6%9C%8D%E5%8A%A1%E8%B7%AF%E7%94%B1%E6%96%B9%E6%A1%88%EF%BC%89))
- [x] 基于共享内存的服务端限流，支持多维度的限流，支持服务或者接口级别的限流([FreqControl](https://github.com/dapeng-soa/dapeng-soa/wiki/DapengFreqControl))
- [x] 分布式服务调用日志跟踪

# Next
[Quick start](https://github.com/dapeng-soa/dapeng-soa/blob/master/quickstart.md)

# Documents
- [x] [中文](https://github.com/dapeng-soa/dapeng-soa/blob/master/README.md)
- [x] [English](https://github.com/dapeng-soa/dapeng-soa/blob/master/README_en.md)
