package com.github.dapeng.pinpoint.plugin.interceptor;

import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.SoaSystemEnvProperties;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.pinpoint.plugin.DapengConstants;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.trace.ServiceType;

import static com.github.dapeng.pinpoint.plugin.DapengConstants.DAPENG_MONITOR_SERVICE;

/**
 * Created by tangliu on 16/12/7.
 */
public class DapengProviderInterceptor extends SpanSimpleAroundInterceptor {

    public static PLogger logger = PLoggerFactory.getLogger(DapengProviderInterceptor.class);

    public DapengProviderInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor, DapengProviderInterceptor.class);
    }

    @Override
    protected void doInBeforeTrace(SpanRecorder recorder, Object o, Object[] objects) {

        SoaHeader soaHeader = TransactionContext.Factory.getCurrentInstance().getHeader();

        //记录ServiceType
        recorder.recordServiceType(DapengConstants.DAPENG_PROVIDER_SERVICE_TYPE);

        //记录rpc name, endPoint, RemoteAddress
        recorder.recordRpcName(soaHeader.getServiceName() + ":" + soaHeader.getMethodName());
        recorder.recordEndPoint(SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT);
        recorder.recordRemoteAddress(soaHeader.getCallerIp().orElse("unknown"));

        // If this transaction did not begin here, record parent(client who sent this request) information
        if (!recorder.isRoot()) {
            String parentApplicationName = soaHeader.getAttachment(DapengConstants.META_PARENT_APPLICATION_NAME);

            if (parentApplicationName != null) {
                short parentApplicationType = NumberUtils.parseShort(soaHeader.getAttachment(DapengConstants.META_PARENT_APPLICATION_TYPE), ServiceType.UNDEFINED.getCode());
                recorder.recordParentApplication(parentApplicationName, parentApplicationType);

                // Pinpoint finds caller - callee relation by matching caller's end point and callee's acceptor host.
                // https://github.com/naver/pinpoint/issues/1395
                recorder.recordAcceptorHost(SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT);
            }
        }

    }

    @Override
    protected Trace createTrace(Object o, Object[] objects) {

        //do not trace MonitorService
        SoaHeader soaHeader = TransactionContext.Factory.getCurrentInstance().getHeader();
        if (soaHeader.getServiceName().equals(DAPENG_MONITOR_SERVICE))
            return traceContext.disableSampling();

        // If this transaction is not traceable, mark as disabled.
        if (soaHeader.getAttachment(DapengConstants.META_DO_NOT_TRACE) != null) {
            return traceContext.disableSampling();
        }

        String transactionId = soaHeader.getAttachment(DapengConstants.META_TRANSACTION_ID);

        // If there's no trasanction id, a new trasaction begins here.
        if (transactionId == null) {
            return traceContext.newTraceObject();
        }

        long parentSpanId = NumberUtils.parseLong(soaHeader.getAttachment(DapengConstants.META_PARENT_SPAN_ID), SpanId.NULL);
        long spanId = NumberUtils.parseLong(soaHeader.getAttachment(DapengConstants.META_SPAN_ID), SpanId.NULL);
        short flags = NumberUtils.parseShort(soaHeader.getAttachment(DapengConstants.META_FLAGS), (short) 0);

        TraceId traceId = traceContext.createTraceId(transactionId, parentSpanId, spanId, flags);

        return traceContext.continueTraceObject(traceId);
    }

    @Override
    protected void doInAfterTrace(SpanRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {

        recorder.recordApi(methodDescriptor);

        SoaHeader soaHeader = TransactionContext.Factory.getCurrentInstance().getHeader();

        //记录请求和返回结果
        recorder.recordAttribute(DapengConstants.DAPENG_ARGS_ANNOTATION_KEY, formatToString(soaHeader.getAttachment(DapengConstants.DAPENG_ARGS)));

        if (throwable == null) {
            recorder.recordAttribute(DapengConstants.DAPENG_RESULT_ANNOTATION_KEY, formatToString(soaHeader.getAttachment(DapengConstants.DAPENG_RESULT)));
        } else {
            recorder.recordException(throwable);
        }
    }


    public static String formatToString(String msg) {
        if (msg == null)
            return msg;

        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (len > max_len)
            msg = msg.substring(0, 128) + "...(" + len + ")";

        return msg;
    }


}
