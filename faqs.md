## 1.容器启动时报IP获取错误
```
Caused by: java.lang.RuntimeException: wrong with get ip
              at: com.github.dapeng.core.helper.IPUtils.<init>(IPUtils.java:23)
```

一般出现在linux环境。

解决方案：在/etc/hosts中绑定本地内网ip跟机器名
以centos为例：
>echo $(ifconfig eth0|grep -w "inet"|awk '{print $2}') `hostname` >> /etc/hosts

Mac 环境

> echo $(ifconfig en0 | grep -w "inet"|awk '{print $2}') `hostname` 

## 2.IDL中服务接口注释，在文档站点中解析错误
注释需符合markdown的规范，例如
```
    /**
    # 1. 生成在线订单
    ## 业务描述
        生成在线订单
    ## 接口依赖
        无
    ## 边界异常说明
        无
    ## 输入
        TOrderRequest request
    ## 数据库变更
        1. 主单入库
        2. 子单入库
    ##  事务处理
        无
    ## 前置检查
       1. 判断订单是否存在, 如果订单已存在，提示异常: 订单已存在
       2. xxx
       3. xxx
       4. xxx

    ## 逻辑处理

        1. 插入订单
        2. 插入子单
        3. 发送CreateOrderEvent 事件通知消费者
    ## 输出
        无
    */
    void createOrder(1: order_request_new.TCreateOrderRequestNew request)
    (events="com.today.api.order.events.CreateOrderEventNew")
```
>上述的错误范例，在接口文档注释中，标题指示符#应在每行的开头(前面不能有任何字符包括空格)

正确版本如下:
```
    /**
# 1. 生成在线订单
## 业务描述
        生成在线订单
## 接口依赖
        无
## 边界异常说明
        无
## 输入
        TOrderRequest request
## 数据库变更
        1. 主单入库
        2. 子单入库
##  事务处理
        无
## 前置检查
       1. 判断订单是否存在, 如果订单已存在，提示异常: 订单已存在
       2. xxx
       3. xxx
       4. xxx

## 逻辑处理

        1. 插入订单
        2. 插入子单
        3. 发送CreateOrderEvent 事件通知消费者
## 输出
        无
    */
    void createOrder(1: order_request_new.TCreateOrderRequestNew request)
    (events="com.today.api.order.events.CreateOrderEventNew")
```

