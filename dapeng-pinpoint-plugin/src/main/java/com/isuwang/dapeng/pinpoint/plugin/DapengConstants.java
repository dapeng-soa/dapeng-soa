package com.github.dapeng.pinpoint.plugin;

import com.navercorp.pinpoint.common.trace.*;

import static com.navercorp.pinpoint.common.trace.AnnotationKeyProperty.VIEW_IN_RECORD_SET;

/**
 * Created by tangliu on 16/12/5.
 */
public class DapengConstants {

    public static ServiceType DAPENG_PROVIDER_SERVICE_TYPE = ServiceTypeFactory.of(1999, "DAPENG_PROVIDER", ServiceTypeProperty.RECORD_STATISTICS);
    public static ServiceType DAPENG_CONSUMER_SERVICE_TYPE = ServiceTypeFactory.of(9999, "DAPENG_CONSUMER", ServiceTypeProperty.RECORD_STATISTICS);
    public static AnnotationKey DAPENG_ARGS_ANNOTATION_KEY = AnnotationKeyFactory.of(900, "dapeng.args", VIEW_IN_RECORD_SET);
    public static AnnotationKey DAPENG_RESULT_ANNOTATION_KEY = AnnotationKeyFactory.of(999, "dapeng.result", VIEW_IN_RECORD_SET);

    public static String META_DO_NOT_TRACE = "_DAPENG_DO_NOT_TRACE";
    public static String META_TRANSACTION_ID = "_DAPENG_TRASACTION_ID";
    public static String META_SPAN_ID = "_DAPENG_SPAN_ID";
    public static String META_PARENT_SPAN_ID = "_DAPENG_PARENT_SPAN_ID";
    public static String META_PARENT_APPLICATION_NAME = "_DAPENG_PARENT_APPLICATION_NAME";
    public static String META_PARENT_APPLICATION_TYPE = "_DAPENG_PARENT_APPLICATION_TYPE";
    public static String META_FLAGS = "_DAPENG_FLAGS";

    public static String DAPENG_ARGS = "dapeng_args";
    public static String DAPENG_RESULT = "dapeng_result";

    public static String DAPENG_MONITOR_SERVICE = "com.github.dapeng.monitor.api.service.MonitorService";
}
