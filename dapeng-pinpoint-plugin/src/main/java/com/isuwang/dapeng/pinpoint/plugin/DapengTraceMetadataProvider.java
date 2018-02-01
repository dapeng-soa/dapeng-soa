package com.github.dapeng.pinpoint.plugin;

import com.navercorp.pinpoint.common.trace.TraceMetadataProvider;
import com.navercorp.pinpoint.common.trace.TraceMetadataSetupContext;

/**
 * Created by tangliu on 16/12/2.
 */
public class DapengTraceMetadataProvider implements TraceMetadataProvider {

    @Override
    public void setup(TraceMetadataSetupContext context) {

        context.addServiceType(DapengConstants.DAPENG_PROVIDER_SERVICE_TYPE);
        context.addServiceType(DapengConstants.DAPENG_CONSUMER_SERVICE_TYPE);
        context.addAnnotationKey(DapengConstants.DAPENG_ARGS_ANNOTATION_KEY);
        context.addAnnotationKey(DapengConstants.DAPENG_RESULT_ANNOTATION_KEY);
    }
}
