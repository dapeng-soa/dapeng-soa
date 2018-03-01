package com.github.dapeng.metadata;

import com.github.dapeng.core.SoaConnectionPool;
import com.github.dapeng.core.SoaConnectionPoolFactory;
import com.github.dapeng.core.SoaException;

import java.util.ServiceLoader;

/**
 * Created by tangliu on 2016/3/3.
 */
public class MetadataClient {

    private final String serviceName;
    private final String version;
    private final String methodName = "getServiceMetadata";

    private SoaConnectionPool pool;

    public MetadataClient(String serviceName, String version) {
        this.serviceName = serviceName;
        this.version = version;

        ServiceLoader<SoaConnectionPoolFactory> factories = ServiceLoader.load(SoaConnectionPoolFactory.class);
        for (SoaConnectionPoolFactory factory : factories) {
            this.pool = factory.getPool();
            break;
        }
        this.pool.registerClientInfo(serviceName, version);

    }

    /**
     * getServiceMetadata
     **/
    public String getServiceMetadata() throws SoaException {
        getServiceMetadata_result result = pool.send(serviceName, version, methodName,
                new getServiceMetadata_args(),
                new GetServiceMetadata_argsSerializer(),
                new GetServiceMetadata_resultSerializer());

        return result.getSuccess();
    }
}
