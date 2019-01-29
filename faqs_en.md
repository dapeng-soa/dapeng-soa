## 1.get ip error when container boots up
```
Caused by: java.lang.RuntimeException: wrong with get ip
              at: com.github.dapeng.core.helper.IPUtils.<init>(IPUtils.java:23)
```

Always occurs within Linux。

Solution：Bind hostname and ip address in /etc/hosts
Take centos for example：
>echo $(ifconfig eth0|grep "inet "|awk '{print $2}') `hostname` >> /etc/hosts


## 2.apiDoc site can't parse comments of service within IDL
Comments should be up to standard of markdown, such as:
Wrong version:
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
>Title instruction '#' should be the first char of thes line

Correct version:
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