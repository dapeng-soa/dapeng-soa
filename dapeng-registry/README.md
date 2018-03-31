

## 配置途径及其优先级
所有可配置项,皆有多种途径进行配置.默认配置途径以及优先级从大到小分别为:
1. Invocation中设置值, 每次服务调用可以设置不同值
2. Option中设置值(命令行启动参数或者环境变量)
3. 配置中心配置值
4. IDL中配置值
5. 默认值

`ZK数据结构`
```
/soa/config/services[global configs]
               |
                --->{serviceName}[service configs]
                         |
                          --->{host}:{port}:{version}[service configs for host]
```
> 其中,全局配置项放在/soa/config/services节点

> 具体服务的配置值放在/soa/config/services/{serviceName}

> 针对特定Host的服务的配置值放在/soa/config/services/{serviceName}/{host}:{port}:{version}

全局配置信息会写入到配置库中, 由配置中心监听zk的启动, 在zk启动的时候把配置信息写入到zk的全局配置节点中.

### ZK超时设置
这个值作为/soa/runtime/services/{serviceName}节点对应的data值.
> timeout/800ms,createSupplier:100ms,modifySupplier:200ms;

### ZK负载均衡设置
这个值作为/soa/runtime/services/{serviceName}节点对应的data值.
> loadbalance/LeastActive,createSupplier:Random,modifySupplier:RoundRobin;


## 范例


## ZK 配置格式

### 全局配置，对所有服务

```$xslt


目标节点：/soa/config/services

节点data：timeout/800ms;loadBalance/random

```

### 服务级别配置，可以细粒化到每一个方法的配置

```
目标节点: /soa/config/services/com.github.dapeng.user.service.UserService 

节点data：timeout/800ms,register:4001ms,modifySupplier:200ms;loadBalance/leastActive,createSupplier:random,modifySupplier:roundRobin;

```