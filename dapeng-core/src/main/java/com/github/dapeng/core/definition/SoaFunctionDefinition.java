package com.github.dapeng.core.definition;

import com.github.dapeng.core.*;
import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.SoaException;

import java.util.concurrent.Future;

/**
 * Created by lihuimin on 2017/12/14.
 */
public abstract class SoaFunctionDefinition<I, REQ, RESP>  {

    public static abstract class Sync<I, REQ, RESP> extends SoaFunctionDefinition<I, REQ, RESP> {

        public Sync(String methodName, BeanSerializer<REQ> reqSerializer, BeanSerializer<RESP> respSerializer,CustomConfigInfo customConfigInfo){
            super(methodName, reqSerializer, respSerializer,customConfigInfo);
        }

        public abstract RESP apply(I iface, REQ req) throws SoaException;
    }

    public static abstract class Async<I, REQ, RESP> extends SoaFunctionDefinition<I, REQ, RESP> {

        public Async(String methodName, BeanSerializer<REQ> reqSerializer, BeanSerializer<RESP> respSerializer,CustomConfigInfo customConfigInfo){
            super(methodName, reqSerializer, respSerializer,customConfigInfo);
        }

        public abstract Future<RESP> apply(I iface, REQ req) throws SoaException;
    }

    public final String methodName;
    public final BeanSerializer<REQ> reqSerializer;
    public final BeanSerializer<RESP> respSerializer;
    public final CustomConfigInfo customConfigInfo;


    public SoaFunctionDefinition(String methodName, BeanSerializer<REQ> reqSerializer, BeanSerializer<RESP> respSerializer,CustomConfigInfo customConfigInfo) {
        this.methodName = methodName;
        this.reqSerializer = reqSerializer;
        this.respSerializer = respSerializer;
        this.customConfigInfo = customConfigInfo;
    }


}
