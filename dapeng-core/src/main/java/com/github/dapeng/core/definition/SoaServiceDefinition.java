package com.github.dapeng.core.definition;

import com.github.dapeng.core.CustomConfigInfo;

import java.util.Map;
import java.util.Optional;

/**
 * @author lihuimin
 * @date 2017/12/14
 */
public class SoaServiceDefinition<I> {
    public final I iface;    // sync interface
    public final Class<I> ifaceClass;

    public final Map<String, SoaFunctionDefinition<I, ?, ?>> functions;

    public final boolean isAsync;

    private Optional<CustomConfigInfo> configInfo = Optional.empty();

    public Optional<CustomConfigInfo> getConfigInfo() {

        return configInfo;
    }

    public void setConfigInfo(CustomConfigInfo configInfo) {
        this.configInfo = Optional.of(configInfo);
    }

    public SoaServiceDefinition(I iface, Class<I> ifaceClass,
                                Map<String, SoaFunctionDefinition<I, ?, ?>> functions) {
        this.iface = iface;
        this.ifaceClass = ifaceClass;
        this.isAsync = AsyncService.class.isAssignableFrom(iface.getClass());
        this.functions = functions;

        // TODO assert functions.forall( _.isInstance[ SoaFunctionDefinition.Async] )
    }

    public Map<String, SoaFunctionDefinition<I, ?, ?>> buildMap(SoaFunctionDefinition... functions) {
        return null;
    }


}
