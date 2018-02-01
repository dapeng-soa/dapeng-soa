# pinpoint插件实战

## 资源文件

pinpoint agent在启动的时候，会加载`plugin`文件夹下所有的插件。它会扫描插件jar包中`META-INF/services`目录下的两个配置文件来确认`ProfilerPlugin`和`TraceMetadataProvider`的实现类。
`META-INF/services/com.naercorp.pinpoint.bootstrap.plugin.ProfilerPlugin`:
```
com.github.dapeng.pinpoint.plugin.DapengPinpintPlugin
```

`META-INF/services/com.navercorp.pinpoint.common.trace.TraceMetadataProvider`:
```
com.github.dapeng.pinpoint.plugin.DapengTraceMetadataProvider
```

## TraceMetadataProvider

只需要实现`setup`方法，添加`ServiceType`和`AnnotationKey`, 主要用于服务类型和记录的数据的标识，`agent`上送给`collector`, `collector`和`web`通过它们区分不同的服务节点类型。

```
public class DapengTraceMetadataProvider implements TraceMetadataProvider {


    public static ServiceType DAPENG_PROVIDER_SERVICE_TYPE = ServiceTypeFactory.of(1999, "DAPENG_PROVIDER", RECORD_STATISTICS);
    public static ServiceType DAPENG_CONSUMER_SERVICE_TYPE = ServiceTypeFactory.of(9999, "DAPENG_CONSUMER", RECORD_STATISTICS);
    public static AnnotationKey DAPENG_ARGS_ANNOTATION_KEY = AnnotationKeyFactory.of(900, "dapeng.args", VIEW_IN_RECORD_SET);
    public static AnnotationKey DAPENG_RESULT_ANNOTATION_KEY = AnnotationKeyFactory.of(999, "dapeng.result", VIEW_IN_RECORD_SET);

    @Override
    public void setup(TraceMetadataSetupContext context) {

        context.addServiceType(DAPENG_PROVIDER_SERVICE_TYPE);
        context.addServiceType(DAPENG_CONSUMER_SERVICE_TYPE);
        context.addAnnotationKey(DAPENG_ARGS_ANNOTATION_KEY);
        context.addAnnotationKey(DAPENG_RESULT_ANNOTATION_KEY);
    }
}
```
> 注意这里的ServiceType和AnnotationKey的code是有范围的.
> `RECORD_STATICSTICS`类型的ServiceType, agent会统计它的耗时。
> `VIEW_IN_RECORD_SET`属性的AnnotationKey将会在调用树状图中行显示。

## ProfilerPlugin

插件必须实现`ProfilerPlugin`接口。也只需要实现一个`setup`方法。在这个方法里，我们需要做两件事：
1. 添加应用类型检测器
2. 给指定类添加注册`TransformCallback`类
```
@Override
public void setup(ProfilerPluginSetupContext context) {
    addApplicationTypeDetector(context);  
    addTransformers();
}
```

### 应用类型检测器

`agent`通过这个检测器，来确定当前应用节点的服务类型

```
private void addApplicationTypeDetector(ProfilerPluginSetupContext context, DapengConfiguration config) {
        context.addApplicationTypeDetector(new DapengProviderDetector());
}

public class DapengProviderDetector implements ApplicationTypeDetector {

    private static final String DEFAULT_BOOTSTRAP_MAIN = "com.github.dapeng.bootstrap.Bootstrap";

    @Override
    public ServiceType getApplicationType() {
        return DapengConstants.DAPENG_PROVIDER_SERVICE_TYPE;
    }

    @Override
    public boolean detect(ConditionProvider provider) {
        return provider.checkMainClass(Arrays.asList(DEFAULT_BOOTSTRAP_MAIN));
    }
}

```
简单一点来说，agent会检测你这个应用的启动主方法所在的类，来确定你这个应用节点的类型。`dapeng`容器通过`com.github.dapeng.bootstrap.Bootstrap`的`main`方法启动，agent检测到了这个启动类，便认为你这个应用属于`DAPENG_PROVIDER_SERVICE_TYPE`。同理，判断是否为`tomcat`应用，则看主启动类是否`org.apache.catalina.startup.Bootstrap`(严谨点还有其他判断条件)。

### TransformCallback

插件给指定类注册`TransformCallback`类。类加载器在加载类时，如果该类名（全路径）上注册有`TransformCallback`,那么会调用`TransformCallback`的`doInTransform`方法，对类进行字节码注入，然后将修改后的字节码返回给类加载器加载。我们要做的就是在指定类的指定方法里面，记录`transaction,Span`信息等。

#### 消费端类（Consumer）
```
 transformTemplate.transform("com.github.dapeng.remoting.BaseServiceClient", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String s, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            target.getDeclaredMethod("sendBase", "java.lang.Object", "java.lang.Object", "com.github.dapeng.core.TBeanSerializer", "com.github.dapeng.core.TBeanSerializer").addInterceptor("com.github.dapeng.pinpoint.plugin.interceptor.DapengConsumerInterceptor");

            return target.toBytecode();
            }
        });
```
这段代码的意思是，给`BaseServiceClient`注册`TransformCallback`,类加载时，`agent`会找到`BaseServiceClient`中的`sendBase`方法，使用`DapengConsumerInterceptor`对该方法进行注入。最后返回修改后的字节码。

现在再看看`DapengConsumerInterceptor`,它实现了`AroundInterceptor4`, 主要实现了`before`和`after`方法。可以理解为，`before`中的内容插入到了`sendBase`方法最前面，而`after`中内容则是`sendBase`执行完后执行。

```
@Override
public void before(Object o, Object o1, Object o2, Object o3, Object o4) 
    Trace trace = traceContext.currentTraceObject();
    if (trace == null) {
        trace = traceContext.newTraceObject();
    }
    if (trace == null)
        return;
```
> 按照官方的sample工程，这里只需要`currentTraceObject`然后判断是否为`null`,是则退出。然而在实际使用中，若调用`BaseServiceClient.sendBase`的节点之前并没有开启一个“事务追踪”, 那么这次调用就不会被记录。比如一个定时器，它直接调用了这个方法，而不同于`tomcat`中的`controller`调用这个方法，`controller`在进入这个方法前，已经由`tomcat`的插件或者其他插件开启了一个“事务追踪”。所以我们这里判断，如果为`null`, 则使用`newTraceObject`开启新的事务。

```
if (trace.canSampled()) {

    SpanEventRecorder recorder = trace.traceBlockBegin();
    recorder.recordServiceType(DapengConstants.DAPENG_CONSUMER_SERVICE_TYPE);
    recorder.recordRpcName(soaHeader.getServiceName() + ":" + soaHeader.getMethodName());

    //因为这里是要将数据传给下一个节点（dapeng-provider）,所以用nextTraceId
    TraceId nextId = trace.getTraceId().getNextTraceId();
    recorder.recordNextSpanId(nextId.getSpanId());
    
    soaHeader.setAttachment(DapengConstants.META_TRANSACTION_ID, nextId.getTransactionId());
    soaHeader.setAttachment(DapengConstants.META_SPAN_ID, Long.toString(nextId.getSpanId()));
    soaHeader.setAttachment(DapengConstants.META_PARENT_SPAN_ID, Long.toString(nextId.getParentSpanId()));
    soaHeader.setAttachment(DapengConstants.META_PARENT_APPLICATION_TYPE, Short.toString(traceContext.getServerTypeCode()));
    soaHeader.setAttachment(DapengConstants.META_PARENT_APPLICATION_NAME, traceContext.getApplicationName());
    soaHeader.setAttachment(DapengConstants.META_FLAGS, Short.toString(nextId.getFlags()));
} else {
    // If sampling this transaction is disabled, pass only that infomation to the server.
    soaHeader.setAttachment(DapengConstants.META_DO_NOT_TRACE, "1");
}
```

在这里，我们需要记录一些`Span`必需的信息，至于具体要记录什么，可以参考[pinpoint插件原理学习](http://www.ltang.me/2016/12/09/pinpoint-plugin-study/)。并把追踪信息传递给下一个节点，在这里，下一个节点就是我们的`dapengServer/DAPENG_PROVIDER`.

> 值得注意的是,怎么将`Trace, Span`信息传递给下一个节点并不是`pinpoint`关心的事，也就是说，你需要自己实现这些信息从`consumer`到`provider`的传递。为了更简单的实现这个传递，我直接修改了`Dapeng`框架的代码，使得这些追踪信息能够通过`SoaHeader`随请求一起传递到服务端。

```
@Override
public void after(Object target, Object o1, Object o2, Object o3, Object o4, Object result, Throwable throwable) {

    Trace trace = traceContext.currentTraceObject();
    if (trace == null) return;
    try {
        SpanEventRecorder recorder = trace.currentSpanEventRecorder();
        recorder.recordApi(descriptor);
        if (throwable == null) {
            InvocationContext context = InvocationContext.Factory.getCurrentInstance();
            String endPoint = context.getCalleeIp() + ":" + context.getCalleePort();
            recorder.recordEndPoint(endPoint);
            recorder.recordDestinationId(endPoint);
            //记录请求参数和返回结果
            recorder.recordAttribute(DapengConstants.DAPENG_ARGS_ANNOTATION_KEY, o1);
            recorder.recordAttribute(DapengConstants.DAPENG_RESULT_ANNOTATION_KEY, result);
        } else {
            recorder.recordException(throwable);
        }
    } finally {
        trace.traceBlockEnd();
    }
}
```
> 需要注意的是，`pinpoint`通过`consumer`的`endPoint/destinationId`和`provider`的`acceptorHost`将不同的服务节点关联，并显示在结构图上，所以这两个值应该一致。

#### 服务端类（Provider）

```
 transformTemplate.transform("com.github.dapeng.core.SoaBaseProcessor", (instrumentor, loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
    InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

    target.getDeclaredMethod("process", "com.github.dapeng.org.apache.thrift.protocol.TProtocol", "com.github.dapeng.org.apache.thrift.protocol.TProtocol").addInterceptor("com.github.dapeng.pinpoint.plugin.interceptor.DapengProviderInterceptor");

    return target.toBytecode();
});
```

> 必须要说的是，需要修改的方法，不能是一个抽象方法。刚开始我企图在一个更底层的抽象方法上进行`transform`, 启动的时候报错了。我本来以为它会在所有实现这个抽象方法的方法上进行`transform`,但看来并非如此，只能通过指定的类进行`transform`。

来看一下`DapengProviderInterceptor`,关键代码都有注释了:

```
public class DapengProviderInterceptor extends SpanSimpleAroundInterceptor {
...
@Override
protected Trace createTrace(Object o, Object[] objects) {

    // 如果上一个节点表明此节点不可追踪，那么直接disable
    if (soaHeader.getAttachment(DapengConstants.META_DO_NOT_TRACE) != null) {
        return traceContext.disableSampling();
    }
    String transactionId = soaHeader.getAttachment(DapengConstants.META_TRANSACTION_ID);
    // 如果上一个节点没有传递transactionId,那么从这里开启一个新的Trace
    if (transactionId == null)  return traceContext.newTraceObject();
    
    long parentSpanId = NumberUtils.parseLong(soaHeader.getAttachment(DapengConstants.META_PARENT_SPAN_ID), SpanId.NULL);
    long spanId = NumberUtils.parseLong(soaHeader.getAttachment(DapengConstants.META_SPAN_ID), SpanId.NULL);
    short flags = NumberUtils.parseShort(soaHeader.getAttachment(DapengConstants.META_FLAGS), (short) 0);
    //看过插件开发指南就知道，TraceId是由transactionId, parentSpanId, spanId, flags构成
    TraceId traceId = traceContext.createTraceId(transactionId, parentSpanId, spanId, flags);
    return traceContext.continueTraceObject(traceId);
}

@Override
protected void doInBeforeTrace(SpanRecorder recorder, Object o, Object[] objects) {

    SoaHeader soaHeader = TransactionContext.Factory.getCurrentInstance().getHeader();
    //记录ServiceType
    recorder.recordServiceType(DapengConstants.DAPENG_PROVIDER_SERVICE_TYPE);
    //记录rpcName, endPoint, RemoteAddress
    recorder.recordRpcName(soaHeader.getServiceName() + ":" + soaHeader.getMethodName());
    recorder.recordEndPoint(SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT);
    recorder.recordRemoteAddress(soaHeader.getCallerIp().orElse("unknown"));//即调用端地址

    //如果事务不是从本节点开始，那么记录父节点信息
    if (!recorder.isRoot()) {
        String parentApplicationName = soaHeader.getAttachment(DapengConstants.META_PARENT_APPLICATION_NAME);

        if (parentApplicationName != null) {
            short parentApplicationType = NumberUtils.parseShort(soaHeader.getAttachment(DapengConstants.META_PARENT_APPLICATION_TYPE), ServiceType.UNDEFINED.getCode());
            recorder.recordParentApplication(parentApplicationName, parentApplicationType);
            //pinpoint通过匹配caller的endPoint和callee的acceptor将它们关联，所以要一致
            // https://github.com/naver/pinpoint/issues/1395
            recorder.recordAcceptorHost(SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT);
        }
    }
}

@Override
protected void doInAfterTrace(SpanRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
    recorder.recordApi(methodDescriptor);
    SoaHeader soaHeader = TransactionContext.Factory.getCurrentInstance().getHeader();
    //记录请求和返回结果
    recorder.recordAttribute(DapengConstants.DAPENG_ARGS_ANNOTATION_KEY, soaHeader.getAttachment(DapengConstants.DAPENG_ARGS));
    if (throwable == null) {
        recorder.recordAttribute(DapengConstants.DAPENG_RESULT_ANNOTATION_KEY, soaHeader.getAttachment(DapengConstants.DAPENG_RESULT));
    } else {
        recorder.recordException(throwable);
    }
}
```

## 使用

这样，一个最基础的pinpoint插件就完成了。我们还可以对它进行扩展，比如对更多的方法进行`transform`, 比如指定特殊的服务／方法调用不需要进行追踪等。具体的功能可以通过[pinpoint-plugin-sample](https://github.com/lioolli/pinpoint-plugin-sample)进行学习。

最后，我们需要将插件打包成jar包，然后放进`agent`的`plugin`文件夹，和`collector/web`的`lib`文件夹。

让我们来看看效果：
![效果图](http://7xnl6z.com1.z0.glb.clouddn.com/pinpoint_map.png)
其中的`basic-servics/other-biz-services`就是我们的`Dapeng`容器。
调用树状图，请求参数等也可以看到了
![调用树状图](http://7xnl6z.com1.z0.glb.clouddn.com/pinpoint_trace.png)

最后，我们还需要做的一件事，就是给你的 `ServiceType`添加图片。
> If you're developing a plugin for applications, you need to add images so the server map can render the corresponding node. The plugin jar itself cannot provide these image files and for now, you will have to add the image files to the web module manually.

> First, put the PNG files to following directories:

> web/src/main/webapp/images/icons (25x25)
> web/src/main/webapp/images/servermap (80x40)
> Then, add ServiceType name and the image file name to htIcons in web/src/main/webapp/components/server-map2/jquery.ServerMap2.js.

## 参考
1. [Pinpoint Plugin Developer Guide](https://github.com/naver/pinpoint/wiki/Pinpoint-Plugin-Developer-Guide)
2. [Pinpoint-plugin-sample](https://github.com/lioolli/pinpoint-plugin-sample)
3. [Dubbo pinpoint插件](https://github.com/naver/pinpoint/tree/master/plugins/dubbo)
