package com.github.dapeng.echo;

import com.github.dapeng.core.*;
import com.github.dapeng.core.helper.DapengUtil;

import java.util.ServiceLoader;

public class echoClient {    private final String serviceName;
    private final String version;
    private final String methodName = "echo";

    private final SoaConnectionPool pool;

    private final SoaConnectionPool.ClientInfo clientInfo;

    public echoClient(String serviceName, String version) {
        this.serviceName = serviceName;
        this.version = version;

        ServiceLoader<SoaConnectionPoolFactory> factories = ServiceLoader.load(SoaConnectionPoolFactory.class, getClass().getClassLoader());
        this.pool = factories.iterator().next().getPool();
        this.clientInfo = this.pool.registerClientInfo(serviceName, version);

    }

    /**
     * echo
     **/
    public String echo() throws SoaException {
        InvocationContextImpl.Factory.currentInstance().sessionTid(DapengUtil.generateTid()).callerMid("InnerApiSite");
        echo_result response = pool.send(serviceName,version,methodName,new echo_args(), new echo_argsSerializer(), new echo_resultSerializer());
        return response.getSuccess();
    }

}
