package com.github.dapeng.pinpoint.plugin.interceptor;

import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.pinpoint.plugin.DapengConstants;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor4;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;

import static com.github.dapeng.pinpoint.plugin.DapengConstants.DAPENG_MONITOR_SERVICE;
import static com.github.dapeng.pinpoint.plugin.interceptor.DapengProviderInterceptor.formatToString;

/**
 * Created by tangliu on 16/12/6.
 */
public class DapengConsumerInterceptor implements AroundInterceptor4 {

    public static PLogger logger = PLoggerFactory.getLogger(DapengConsumerInterceptor.class);

    private final MethodDescriptor descriptor;
    private final TraceContext traceContext;

    public DapengConsumerInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        this.descriptor = descriptor;
        this.traceContext = traceContext;
    }

    @Override
    public void before(Object o, Object o1, Object o2, Object o3, Object o4) {

        //不监控 MonitorService
        SoaHeader soaHeader = InvocationContext.Factory.getCurrentInstance().getHeader();
        if (soaHeader.getServiceName().equals(DAPENG_MONITOR_SERVICE))
            return;

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            trace = traceContext.newTraceObject();
        }
        if (trace == null)
            return;

        if (trace.canSampled()) {

            SpanEventRecorder recorder = trace.traceBlockBegin();

            recorder.recordServiceType(DapengConstants.DAPENG_CONSUMER_SERVICE_TYPE);
            recorder.recordRpcName(soaHeader.getServiceName() + ":" + soaHeader.getMethodName());

            //因为这里是要将数据传给下一个节点（dapeng-provider）,所以用nextTraceId
            TraceId nextId = trace.getTraceId().getNextTraceId();

            recorder.recordNextSpanId(nextId.getSpanId());

            // Finally, pass some tracing data to the server.
            // How to put them in a message is protocol specific.
            // This example assumes that the target protocol message can include any metadata (like HTTP headers).
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
    }

    @Override
    public void after(Object target, Object o1, Object o2, Object o3, Object o4, Object result, Throwable throwable) {

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }
        try {
            SpanEventRecorder recorder = trace.currentSpanEventRecorder();

            recorder.recordApi(descriptor);

            if (throwable == null) {

                InvocationContext context = InvocationContext.Factory.getCurrentInstance();

                String endPoint = context.getCalleeIp() + ":" + context.getCalleePort();

                recorder.recordEndPoint(endPoint);
                recorder.recordDestinationId(endPoint);
                recorder.recordAttribute(DapengConstants.DAPENG_ARGS_ANNOTATION_KEY, formatToString(o1.toString()));
                recorder.recordAttribute(DapengConstants.DAPENG_RESULT_ANNOTATION_KEY, formatToString(result.toString()));

            } else {
                recorder.recordException(throwable);
            }

        } finally {
            trace.traceBlockEnd();
        }
    }
}
