# dapeng 的计数服务模块
针对时序数据进行存储(influxdb存储)，可为dapeng的系统监控和其他业务服务提供统计功能

## 配置参数
可在dapeng配置文件或者命令行/环境变量中配置
```sbtshell
soa.counter.influxdb.url
counter.influxdb.user
counter.influxdb.pwd
```
## 缺省配置
```sbtshell
url:http://127.0.0.1:8086
user:admin
pwd:admin
```