package com.github.dapeng.metadata;

import com.github.dapeng.client.netty.SoaConnectionImpl;
import com.github.dapeng.client.netty.SoaConnectionImpl;
import com.github.dapeng.core.SoaConnectionPool;
import com.github.dapeng.core.SoaConnectionPoolFactory;
import com.github.dapeng.util.SoaSystemEnvProperties;

import java.util.ServiceLoader;

/**
 *
 * @author tangliu
 * @date 2016/3/3
 */
public class MetadataClient {

    private final String serviceName;
    private final String version;
    private final String methodName = "getServiceMetadata";

    private final SoaConnectionPool pool;

    private final SoaConnectionPool.ClientInfo clientInfo;

    public MetadataClient(String serviceName, String version) {
        this.serviceName = serviceName;
        this.version = version;

        ServiceLoader<SoaConnectionPoolFactory> factories = ServiceLoader.load(SoaConnectionPoolFactory.class, getClass().getClassLoader());
        this.pool = factories.iterator().next().getPool();
        this.clientInfo = this.pool.registerClientInfo(serviceName, version);

    }

    /**
     * getServiceMetadata
     **/
    public String getServiceMetadata() throws Exception {
        getServiceMetadata_result result = pool.send(serviceName, version, methodName,
                new getServiceMetadata_args(),
                new GetServiceMetadata_argsSerializer(),
                new GetServiceMetadata_resultSerializer());

        return result.getSuccess();
    }
}
