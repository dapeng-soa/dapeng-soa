include "hello_domain.thrift"

namespace java com.dapeng.example.hello.service

/**
* Hello-Service
**/
service HelloService {

/**
# sayHello
## 业务描述
    sayHello
## 接口依赖
    无
## 边界异常说明
    无
## 输入
    hello
## 前置检查
    无
## 权限检查
    无
## 逻辑处理
    sayHello
## 数据库变更
    无
## 事务处理
    无
## 输出
    string
## 事件
    无
*/
    string sayHello(1:string name),
/**
# sayHello
## 业务描述
    sayHello
## 接口依赖
    无
## 边界异常说明
    无
## 输入
    hello
## 前置检查
    无
## 权限检查
    无
## 逻辑处理
    sayHello
## 数据库变更
    无
## 事务处理
    无
## 输出
    string
## 事件
    无
*/
    string sayHello2(1:hello_domain.Hello hello)

}(group="hello")