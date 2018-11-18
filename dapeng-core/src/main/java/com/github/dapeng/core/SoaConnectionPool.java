package com.github.dapeng.core;

import java.util.concurrent.Future;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaConnectionPool {

    ClientHandle registerClientInfo(String servcice, String version);

    <REQ, RESP> RESP send(
            ClientHandle clientHandle,
            String method,
            REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer) throws SoaException;

    <REQ, RESP> Future<RESP> sendAsync(
            ClientHandle clientHandle,
            String method,
            REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer) throws SoaException;
}
