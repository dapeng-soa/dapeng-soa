/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.echo;

import com.github.dapeng.core.*;
import com.github.dapeng.core.helper.DapengUtil;

import java.util.ServiceLoader;

public class EchoClient {
    private final String serviceName;
    private final String version;
    private final String methodName = "echo";

    private final SoaConnectionPool pool;

    private final SoaConnectionPool.ClientInfo clientInfo;

    public EchoClient(String serviceName, String version) {
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
        echo_result response = pool.send(serviceName, version, methodName, new echo_args(), new echo_argsSerializer(), new echo_resultSerializer());
        return response.getSuccess();
    }

}
