package com.github.dapeng.core;

import java.util.concurrent.Future;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaConnection {

    <REQ, RESP> RESP send(
            ClientHandle clientHandle,
            String method,
            REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer,long timeout) throws SoaException;

    <REQ, RESP> Future<RESP> sendAsync(
            ClientHandle clientHandle,
            String method,
            REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer,
            long timeout) throws SoaException;
}
