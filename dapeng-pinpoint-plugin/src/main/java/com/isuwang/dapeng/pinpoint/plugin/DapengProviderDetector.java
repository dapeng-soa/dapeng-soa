package com.github.dapeng.pinpoint.plugin;

import com.navercorp.pinpoint.bootstrap.plugin.ApplicationTypeDetector;
import com.navercorp.pinpoint.bootstrap.resolver.ConditionProvider;
import com.navercorp.pinpoint.common.trace.ServiceType;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tangliu on 16/12/6.
 */
public class DapengProviderDetector implements ApplicationTypeDetector {

    private static final String DEFAULT_BOOTSTRAP_MAIN = "com.github.dapeng.bootstrap.Bootstrap";

    private List<String> bootstrapMains;

    public DapengProviderDetector(List<String> bootstrapMains) {
        if (bootstrapMains == null || bootstrapMains.isEmpty()) {
            this.bootstrapMains = Arrays.asList(DEFAULT_BOOTSTRAP_MAIN);
        } else {
            this.bootstrapMains = bootstrapMains;
        }
    }

    @Override
    public ServiceType getApplicationType() {
        return DapengConstants.DAPENG_PROVIDER_SERVICE_TYPE;
    }

    @Override
    public boolean detect(ConditionProvider provider) {
        return provider.checkMainClass(bootstrapMains);
    }
}
