package com.github.dapeng.core;

import java.util.concurrent.Future;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaConnectionPool {

    class ClientInfo {
        public final String serviceName;
        public final String version;

        public ClientInfo(String serviceName, String version) {
            this.serviceName = serviceName;
            this.version = version;
        }
    }

    ClientInfo registerClientInfo(String servcice, String version);

    <REQ, RESP> RESP send(
            String service,
            String version,
            String method,
            REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer) throws SoaException;

    <REQ, RESP> Future<RESP> sendAsync(
            String service,
            String version,
            String method,
            REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer) throws SoaException;


//    SoaConnection getConnection() throws SoaException;

//    void removeConnection(SoaConnection connection) throws SoaException;
}
