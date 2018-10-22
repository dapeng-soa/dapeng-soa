<img width="254" src="https://github.com/dapeng-soa/documents/blob/master/images/dapeng-logo/%E5%A4%A7%E9%B9%8Flogo-03.png" alt="dapeng-soa" title="dapeng-soa"/>

[![Language](https://img.shields.io/badge/language-Java-orange.svg)](https://www.oracle.com)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.dapeng-soa/dapeng-parent/badge.svg)](https://search.maven.org/search?q=com.github.dapeng-soa)
[![GitHub release](https://img.shields.io/github/release/dapeng-soa/dapeng-soa.svg)](https://github.com/dapeng-soa/dapeng-soa/releases)
[![DockerHub](https://img.shields.io/badge/docker-dapengsoa-yellow.svg)](https://hub.docker.com/r/dapengsoa/dapeng-container/)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)


Dapeng-soa is a lightweight, high performance micro-service framework, which is based on netty and thrift. It provides service metadata generated from the thrift IDL automatically. Also, scaffolds are provided, such as:
- [x] apiGateway([dapeng-mesh](https://github.com/dapeng-soa/dapeng-mesh)), which transfer json-http request to thrift protocol
- [x] online api documents and testing site([dapeng-api-doc](https://github.com/dapeng-soa/dapeng-api-doc)), which are generated automatically
- [x] command-line tools([dapeng-cli](https://github.com/dapeng-soa/dapeng-cli))
- [x] configuration/deploy server([dapeng-config-server](https://github.com/dapeng-soa/dapeng-config-server))
- [x] maven/sbt plugin for IDEA
- [x] project templates(sbt only by now:g8 template for sbt projects)
- [x] Demo([dapeng-demo](http://demo.dapeng-soa.tech))


# Architecture
<p align="center">
<img src="https://github.com/dapeng-soa/documents/blob/master/images/dapeng-architecture.png" alt="dapeng-soa" title="dapeng-soa"/>
</p>

# Features
- [x] Base on Netty and Thrift
- [x] Service Metadata generated from Thrift IDL
- [x] Service registry and discovery
- [x] Support Java/Scala client code generated
- [x] Support http-json and thrift binary transcode each other
- [x] Full async chain support, included client side and server side
- [x] Smarter service router and loadbalance, routed by http cookie is also supported([Router](https://github.com/dapeng-soa/dapeng-soa/wiki/Dapeng-Service-Route%EF%BC%88%E6%9C%8D%E5%8A%A1%E8%B7%AF%E7%94%B1%E6%96%B9%E6%A1%88%EF%BC%89))
- [x] Frequency control base on MMap.([FreqControl](https://github.com/dapeng-soa/dapeng-soa/wiki/DapengFreqControl))
- [x] Logger trace system for distribute services


# Next
[Quick start](https://github.com/dapeng-soa/dapeng-soa/quickstart_en.md)

# Documents
- [x] [中文](https://github.com/dapeng-soa/dapeng-soa/README.md)
- [x] [English](https://github.com/dapeng-soa/dapeng-soa/README_en.md)