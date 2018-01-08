package com.github.dapeng.metadata;

import com.github.dapeng.client.netty.SoaConnectionImpl;
import com.github.dapeng.client.netty.SoaConnectionImpl;
import com.github.dapeng.util.SoaSystemEnvProperties;

/**
 * Created by tangliu on 2016/3/3.
 */
public class MetadataClient {

    private final String serviceName;
    private final String version;
    private final String methodName = "getServiceMetadata";

    public MetadataClient(String serviceName, String version) {
        this.serviceName = serviceName;
        this.version = version;
    }

    /**
     * getServiceMetadata
     **/
    public String getServiceMetadata() throws Exception {
        getServiceMetadata_result result = new SoaConnectionImpl(SoaSystemEnvProperties.SOA_CONTAINER_IP, SoaSystemEnvProperties.SOA_CONTAINER_PORT)
                .send(serviceName,version,methodName, new getServiceMetadata_args(),
                        new GetServiceMetadata_argsSerializer(),new GetServiceMetadata_resultSerializer());

        return result.getSuccess();
    }


}
