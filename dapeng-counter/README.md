# dapeng 计数服务
针对时序数据进行存储( 默认支持 influxdb ), 可为dapeng的系统监控和其他业务服务提供统计功能

> 注意:监控数据会在本地按照一分钟汇总后上送

## DATABASE
在influxdb新建调用次数，耗时的DATABASE: `dapengState`
在influxdb新建定时任务监控DATABASE: `dapengTask`

## 编写docker-compose文件

> counterService.yml

```yml
version: '2.2'
services:
  counterService:
    container_name: counterService
    image: dapengsoa/counter-service:2.2.1
    restart: on-failure:3
    stop_grace_period: 30s
    environment:
      - serviceName=counterService
      - soa_monitor_enable=false
      - soa_container_port=9797
      - host_ip=${hostIp}
      - soa_counter_influxdb_url=http://${hostIp}:8086
      - TZ=CST-8
      - LANG=zh_CN.UTF-8
      - E_JAVA_OPTS=-Dname=counterService
    volumes:
      - "~/data/logs/counter-service:/dapeng-container/logs"
    ports:
      - "9797:9797"
```

> 注意将${hostIp}换成您自己的ip地址

## 启动计数服务:
```shell
docker-compose -f dapengMeshAuth.yml up -d
```

## 缺省配置
```shell
soa.counter.influxdb.url=http://127.0.0.1:8086
counter.influxdb.user=admin
counter.influxdb.pwd=admin
```

## 上送监控数据
1.开启dapeng服务监控开关
```
# ENV
soa_monitor_enable=true
```
2.启动的dapeng服务

## 监控数据结构
```
DATABASE: dapengState
    - measurement: dapeng_service_process #服务调用监控数据(*_time:耗时,*_calls:调用次数)
        - tags:
            service_name
            method_name
            version_name
            server_ip
            server_port
        - fields:
            i_min_time
            i_max_time
            i_average_time
            i_total_time
            total_calls
            succeed_calls
            fail_calls
    - measurement: dapeng_node_flow #节点流量数据
        - tags:
            node_ip
            node_port
        - fields:
            max_request_flow
            min_request_flow
            sum_request_flow
            avg_request_flow
            max_response_flow
            min_response_flow
            sum_response_flow
            avg_response_flow

# (executeState:执行状态[succeed,failed,triggerTimeOut],costTime:耗时)
DATABASE: dapengTask
    - measurement: dapeng_task_info
        - tags:
            serviceName
            methodName
            versionName
            serverIp
            serverPort
            executeState
        - fields:
            costTime
```

## 监控数据展示
可使用grafana等UI工具进行配置展示
