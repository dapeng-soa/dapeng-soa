package com.github.dapeng.core.definition;

import java.util.Map;

/**
 * Created by lihuimin on 2017/12/14.
 */
public class SoaServiceDefinition<I>{
    public final I iface;    // sync interface
    public final Class<I> ifaceClass;

    public final Map<String, SoaFunctionDefinition<I,?,?>> functions;

    public final boolean isAsync;

    public SoaServiceDefinition(I iface, Class<I> ifaceClass,
                                Map<String, SoaFunctionDefinition<I, ?, ?>> functions){
        this.iface = iface;
        this.ifaceClass = ifaceClass;
        this.isAsync = AsyncService.class.isAssignableFrom(iface.getClass());
        this.functions = functions;

        // TODO assert functions.forall( _.isInstance[ SoaFunctionDefinition.Async] )
    }

    public Map<String, SoaFunctionDefinition<I,?,?>> buildMap(SoaFunctionDefinition ...functions){
        return null;
    }


}
