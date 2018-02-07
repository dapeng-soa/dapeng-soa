# dapeng-monitor-filter
本项目为dapeng提供了QPS相关的数据统计监控能力.
本项目仅提供数据统计功能,

数据存储依赖模块
```sbtshell
dapeng-counter-api
```

### 在基于dapeng-soa的项目中使用此监控插件

```sbtshell
==> build.sbt

"com.github.dapeng" % "dapeng-monitor-filter" % "2.0.0-SNAPSHOT"
```
### 本项目包括的filter如下
```$xslt
com.github.dapeng.monitor.filter.QpsFilter
com.github.dapeng.monitor.filter.ServiceProcessFilter
```
### filter配置参数(可选, 不配置的话默认加载全部插件. 在dapeng配置文件或者命令行/环境变量中配置)
```$xslt
排除或者引入指定的filter
soa.filter.excludes=
soa.filter.includes=com.github.dapeng.monitor.filter.QpsFilter

```
### 监控配置参数(在dapeng配置文件或者命令行/环境变量中配置)
```$xslt
#监控数据存储库
soa.monitor.infulxdb.database=dapengState
#qps的上送间隔,单位秒
soa.monitor.qps.period=5
#服务调用统计的上送间隔
soa.monitor.service.period=60
```
### 缺省
- filter的加载:
  缺省加载所有filter;
  如果soa.filter.includes有配置,那么只加载soa.filter.includes指定的filters;
  如果配置了soa.filter.excludes,那么会排除该参数指定的filters.
  如果同时配置了上述两个参数, 那么只有soa.filter.includes有效
- 监控数据存储库 dapengState,可配置
- qps存储表dapeng_qps(暂不可配置)
- 服务调用统计存储表dapeng_service_process(暂不可配置)
- qps上送间隔5000ms
- service_process上送间隔60000ms