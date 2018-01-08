# 大鹏重构

[TOC]

### 大鹏容器重构
思路:
1. 一个大的容器内包含所有容器所需的插件,具体应用项目的信息,
    1.1. 容器负责加载插件(Zookeeper, Netty, ScheduledTask etc.) 以及 AppLoader（如 Spring）
    1.2. AppLoader负责加载应用程序(SpringContainer as AppLoader)
    1.3. 插件基于事件处理特定的逻辑(如 服务注册，卸载)
2. 容器可以管理 应用程序的注册，卸载， 插件的注册，卸载

<b>基于该思路，dapeng容器架构重新定义以下几个主要的元素</b>
- 1 IContainer
- 2 IApplication
    - 2.1 ServiceInfo
- 3 AppLoader
- 4 IPlugin 
- 5 Event
- 5.1 AppEvent
- 5.2 PluginEvent(暂时不需要)
- 6 Listener

##### 2.1 容器主要元素的类图
![](https://ws2.sinaimg.cn/large/006tKfTcgy1fmc1j6vui2j31b00isdla.jpg)
![](https://ws4.sinaimg.cn/large/006tKfTcgy1fmc1e2yavqj310i08875g.jpg)
![](https://ws1.sinaimg.cn/large/006tKfTcgy1fmc22ui2skj31io0ju0xi.jpg)

##### 2.2 容器初始化的时序图
该时序图中的Plugin只是列出了两种不同类型的Plugin:
1: 需要监听AppEvent(ZookeeperRegisterPlugin); 
2: 不需要监听AppEvent(FilterPlugin)
实际上在容器初始化的时候还有其他的Plugin，在此就不一一画到时序图中了，大体是一致的.
>在dapeng框架中, 
<b>目前监听AppEvent的插件</b>: `ZookeeperRegistryPlugin`, `LocalRegistryPlugin`, `ScheduledTaskPlugin`, `NettyPlugin`.
<b>不需要监听AppEvent的插件</b>: `LogbackContainer`, `PluginContainer`, `FilterContainer`, `VersionContainer`, `TransactionContainer`

![](https://ws4.sinaimg.cn/large/006tKfTcgy1fmbrd0ils2j31fs0wywi7.jpg)


### dapeng服务端重构

标签（空格分隔）： dapeng
---

dapeng服务端处理流程为
 1. 读取报文（readMessageBegin），使用SoaMessageProcessor类来处理，stx,version,seqid,protocol,soaHeader。
 2. 从容器拿到对应的Processor()。（新版的获取方法，旧的是启动netty服务端的时候把ProcessorCache作为参数传的）
 3. 获取相应的Dispatcher，执行对应的processRequest方法，主要是使用和不使用线程池两个继承的接口。
 
processRequest方法中
 1. 使用SoaMessageProcessor来序列化接口调用的结果。
 2. 判断使用同步还是异步，从processor中获取requestSerializer和responseSerializer,requestSerializer读取参数，并调用对应接口，使用responseSerializer对得到的返回结果序列化并返回给客户端。

伪代码如下：
 
```java
public class SoaServerHandler {

    void readRequestHeader(ChannelHandlerContext ctx, ByteBuf message) throws TException {

        TSoaTransport soaTransport = new TSoaTransport(message);
        SoaMessageProcessor parser = new SoaMessageProcessor(false,soaTransport);
        parser.parseSoaMessage();
        // parser.service, version, method, header, bodyProtocol

        TransactionContext context = TransactionContext.Factory.getCurrentInstance();
        SoaHeader soaHeader = context.getHeader();
        fillTranscationContex(context);

        //APlugin.markRequestBegin(); // container.registerFilter(...); container.startThread(...);
        SoaServiceDefinition processor = processors.get(new ProcessorKey(soaHeader.getServiceName(),soaHeader.getVersionName()));

        ExtensionCenter.getDispatcher().processRequest(ctx,parser,processor,message);
    }
}


public class RequestProcessor {

     public static <I,REQ,RESP> void processRequest(ChannelHandlerContext ctx, SoaMessageProcessor parser, SoaServiceDefinition<I> processor, ByteBuf message) throws TException {
        try {

            ByteBuf byteBuf = ctx.alloc().buffer(8192);
            TSoaTransport transport = new TSoaTransport(byteBuf);
            SoaMessageProcessor builder = new SoaMessageProcessor(false, transport);
            builder.parseSoaMessage();

            TransactionContext context = TransactionContext.Factory.getCurrentInstance();
            SoaHeader soaHeader = context.getHeader();

            SoaFunctionDefinition<I,REQ, RESP> soaFunction = (SoaFunctionDefinition<I,REQ, RESP>)processor.getFunctins().get(soaHeader.getMethodName());
            REQ args = soaFunction.getReqSerializer().read(parser.getContentProtocol());
            parser.getContentProtocol().readMessageEnd();

            HandlerFilter dispatchFilter = new HandlerFilter() {
                @Override
                public void onEntry(FilterContext ctx, FilterChain next) throws TException {
                    if (soaFunction.isAsync()) {
                        CompletableFuture<Object> future = (CompletableFuture<Object>) soaFunction.applyAsync(processor.getIface(), args);
                        ctx.setAttach(this, "response", future);
                    } else {
                        RESP result = null;
                        result =  soaFunction.apply(processor.getIface(), args);
                        context.getHeader().setRespCode(Optional.of("0000"));
                        context.getHeader().setRespMessage(Optional.of("成功"));
                        builder.buildResponse();
                        soaFunction.getRespSerializer().write(result, builder.getContentProtocol());
                        builder.getContentProtocol().writeMessageEnd();

                        ctx.setAttach(this, "response", result);
                    }

                }

                @Override
                public void onExit(FilterContext ctx, FilterChain prev) throws TException {

                    final CompletableFuture<Context> futureResult = new CompletableFuture<>();
                    if (soaHeader.isAsyncCall()) {
                        CompletableFuture<Object> future = (CompletableFuture<Object>) ctx.getAttach(this, "response");
                        future.whenComplete((realResult, ex) -> {
                            if (realResult != null) {
                                AsyncAccept(context, soaFunction, realResult, builder.getContentProtocol(), futureResult);
                            } else {
                                TransactionContext.Factory.setCurrentInstance(context);
                                futureResult.completeExceptionally(ex);
                            }
                        });
                    }

                }
            };

            ContainerManager.getContainer().getSharedChain().setTail(dispatchFilter);
            HandlerFilterContext filterContext = new HandlerFilterContext();
            ContainerManager.getContainer().getSharedChain().onEntry(filterContext);
            ctx.writeAndFlush(byteBuf);
        } finally{
            if (message.refCnt() > 0) {
                message.release();
            }
        }

    }

  }
```
改动的几个点

 - 重构TSoaServiceProtocol,改为SoaMessageParser和SoaMessageBuilder。分别用来读取报文和生成返回结果报文。

 - 是否使用线程池通过扩展的方式来完成。
```java
  public interface IDispatcher {
    
        public void processRequest(ChannelHandlerContext ctx,SoaMessageParser parser, TProcessor processor );
    
    }
    
    不使用线程池
    public class ThreadDispatcher implements IDispatcher {
    
    
        public void processRequest(ChannelHandlerContext ctx, SoaMessageParser parser, Processor processor) {
            Object request = new Object();
           // RequestProcessor.processRequest(request);
        }
    }
    
    使用线程池
    public class ThreadPoolDispatcher implements IDispatcher{
        static class ServerThreadFactory implements ThreadFactory {
            ......
        }
    
        private volatile static ExecutorService executorService = Executors.newFixedThreadPool(SoaSystemEnvProperties.SOA_CORE_POOL_SIZE, new ServerThreadFactory());
    
        public void processRequest(ChannelHandlerContext ctx,SoaMessageParser parser, Processor processor){
    
            final Object request = new Object();
            executorService.execute(()-> RequestProcessor.processRequest(ctx,parser,processor));
    
        }
    }
```
 - 流量控制、数据监控、超时处理等通过filter的方式，并且重构新的filte
 - filter重构
```java
public interface FilterChain {
    // execute current filter's onEntry
    void onEntry(FilterContext ctx);

    // execute current filter's onExit
    void onExit(FilterContext ctx);
}
public interface HandlerFilter {

    void onEntry(FilterContext ctx, FilterChain next);

    void onExit(FilterContext ctx,FilterChain prev);

}

public interface FilterContext {

    void setAttach(HandlerFilter filter, String key, Object value);

    Object getAttach(HandlerFilter filter,String key);

}

public class SharedChain implements FilterChain {

    private HandlerFilter head;
    private HandlerFilter[]shared; // log->a->b->c
    private HandlerFilter tail;
    private int index;  // 0 -> n+2

    public SharedChain(HandlerFilter head, HandlerFilter[] shared, HandlerFilter tail, int index){
        if(index >= 2 + shared.length)
            throw new IllegalArgumentException();
        this.head = head;
        this.shared = shared;
        this.tail = tail;
        this.index = index;

    }

    @Override
    public void onEntry(FilterContext ctx) {
        SharedChain next = null;
        if(index  <= 1 + shared.length)
                next = new SharedChain(head, shared, tail, index+1);
        else next = null;

        if(index == 0) {
            head.onEntry(ctx, next);
        }
        else if(index > 0 && index < shared.length + 1) {
            shared[index-1].onEntry(ctx, next);
        }
        else if(index == shared.length+1) {
            tail.onEntry(ctx, next);
        }
    }

    @Override
    public void onExit(FilterContext ctx) {
        SharedChain prev = null;
        if(index >= 1)
            prev = new SharedChain(head, shared, tail, index - 1);
        else prev = null;

        if(index == 0) {
            head.onExit(ctx, null);
        }
        else if(index > 0 && index < shared.length + 1) {
            shared[index-1].onExit(ctx, prev);
        }
        else if(index == shared.length+1) {
            tail.onEntry(ctx, prev);
        }
    }
}
```
 - 流量控制
  >总并发处理数： 线程池大小
   最大排队数：线程池队列
   对某个IP, callerId,限定并发数为n, 每秒请求数。
```
onEntry:counter(ip).incr, if success next.onEntry else throw excpetion(99)
onExit:
```

 - TProccessor重构 改成SoaServiceDefinition
```java
public class SoaServiceDefinition<I>{
    final I iface;    // sync interface
        final Class<I> ifaceClass;

    final Map<String, SoaFunctionDefinition<I,?,?>> functions;

    public SoaServiceDefinition(I iface,Class<I> ifaceClass, Map<String, SoaFunctionDefinition<I, ?, ?>> functions){
        this.iface = iface;
        this.ifaceClass = ifaceClass;
        this.functions = functions;
    }

    public Map<String, SoaFunctionDefinition<I,?,?>> buildMap(SoaFunctionDefinition ...functions){
        return null;
    }

    public Map<String, SoaFunctionDefinition<I,?,?>> getFunctins (){
        return functions;
    }

    public I getIface() {
        return iface;
    }

    public Class<I> getIfaceClass() {
        return ifaceClass;
    }
}


public abstract class SoaFunctionDefinition<I, REQ, RESP>  {

    final String methodName;
    final TCommonBeanSerializer<REQ> reqSerializer;
    final TCommonBeanSerializer<RESP> respSerializer;
    final boolean isAsync;

    public SoaFunctionDefinition(String methodName, TCommonBeanSerializer<REQ> reqSerializer, TCommonBeanSerializer<RESP> respSerializer, boolean isASync) {
        this.methodName = methodName;
        this.reqSerializer = reqSerializer;
        this.respSerializer = respSerializer;
        this.isAsync=isASync;
    }

    public abstract RESP apply(I iface, REQ req);

    public abstract CompletableFuture<RESP> applyAsync(I iface, REQ req);

    public String getMethodName() {
        return methodName;
    }

    public TCommonBeanSerializer<REQ> getReqSerializer() {
        return reqSerializer;
    }

    public TCommonBeanSerializer<RESP> getRespSerializer() {
        return respSerializer;
    }

    public boolean isAsync() {
        return isAsync;
    }
}

```
 - 超时处理（同步、异步）
    1. Request -> thread, expiredTimestamp, interruptable, interrupCount, doing in filter,
    2. TimeoutThread check queue
        1. interruptable, thread.interrupt
        2. log exception stacktrace
    3. async, thread ( return timeout, future.cancel )

 - 前后端的同步异步调用应该是独立的，目前是由客户端决定后端是同步还是异步调用

 - 流量限制，支持不同的策略：

> 设定某个服务、某个方法的最大并发数、最大排队数
>设定整个容器的最大并发数、最大排队数
>按callerIP设定最大的并发数、每秒最大请求数
>按callerID设定最大并发数、每秒最大请求数

 - 防雪崩

    1. Netty onRead: (timestamp, ByteBuf), TransactionContext. APlugin.staticMethod
    2. last Chain: check current - timestamp, if timeout,...

 
## 标题 ##