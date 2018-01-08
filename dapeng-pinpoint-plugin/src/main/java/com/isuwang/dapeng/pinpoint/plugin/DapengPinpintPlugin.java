package com.github.dapeng.pinpoint.plugin;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

/**
 * Created by tangliu on 16/12/2.
 */
public class DapengPinpintPlugin implements ProfilerPlugin, TransformTemplateAware {

    private static PLogger logger = PLoggerFactory.getLogger(DapengPinpintPlugin.class);

    private TransformTemplate transformTemplate;

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }

    @Override
    public void setup(ProfilerPluginSetupContext context) {

        DapengConfiguration config = new DapengConfiguration(context.getConfig());
        if (!config.isDapengEnabled()) {
            logger.info("Dapeng Plugin disabled");
        }

        this.addApplicationTypeDetector(context, config);
        this.addTransformers();
    }

    private void addTransformers() {

        transformTemplate.transform("com.github.dapeng.remoting.BaseServiceClient", (instrumentor, loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            target.getDeclaredMethod("sendBase", "java.lang.Object", "java.lang.Object", "com.github.dapeng.core.TBeanSerializer", "com.github.dapeng.core.TBeanSerializer").addInterceptor("com.github.dapeng.pinpoint.plugin.interceptor.DapengConsumerInterceptor");

            return target.toBytecode();
        });

        transformTemplate.transform("com.github.dapeng.core.SoaBaseProcessor", (instrumentor, loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            target.getDeclaredMethod("process", "com.github.dapeng.org.apache.thrift.protocol.TProtocol", "com.github.dapeng.org.apache.thrift.protocol.TProtocol").addInterceptor("com.github.dapeng.pinpoint.plugin.interceptor.DapengProviderInterceptor");

            return target.toBytecode();
        });
    }

    /**
     * Pinpoint profiler agent uses this detector to find out the service type of current application.
     */
    private void addApplicationTypeDetector(ProfilerPluginSetupContext context, DapengConfiguration config) {
        context.addApplicationTypeDetector(new DapengProviderDetector(config.getDapengBootstrapMains()));
    }
}