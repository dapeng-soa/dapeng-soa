package com.github.dapeng.service.invoke;

import com.github.dapeng.client.netty.JsonPost;
import com.github.dapeng.client.netty.SoaConnectionPoolImpl;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.service.invoke.entity.BaseRequest;
import com.github.dapeng.util.SoaSystemEnvProperties;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created by lihuimin on 2017/12/6.
 */
public class BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseController.class);

    public static String invoke(BaseRequest baseRequest) {
        if (baseRequest.getServiceName() == null || baseRequest.getVersionName() == null || baseRequest.getMethodName() == null) {
            return String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "Err-Core-098", "serviceName、versionName、methodName信息不能为null", "{}");
        }

        com.github.dapeng.core.metadata.Service service = ApiServices.getService(baseRequest.getServiceName(), baseRequest.getVersionName());
        InvocationContext invocationCtx = InvocationContextImpl.Factory.createNewInstance();
        invocationCtx.setServiceName(baseRequest.getServiceName());
        invocationCtx.setVersionName(baseRequest.getVersionName());
        invocationCtx.setMethodName(baseRequest.getMethodName());
        invocationCtx.setCallerFrom(Optional.of("JsonCaller"));

        fillInvocationCtx(invocationCtx, baseRequest);

        JsonPost jsonPost = new JsonPost(baseRequest.getServiceName(), baseRequest.getMethodName(), true);

        try {
            return jsonPost.callServiceMethod(invocationCtx, baseRequest.getJsonParameter(), service);
        } catch (SoaException e) {

            LOGGER.error(e.getMsg());
            return String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", e.getCode(), e.getMsg(), "{}");

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);
            return String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", "系统繁忙，请稍后再试[9999]！", "{}");
        } finally {
            InvocationContextImpl.Factory.removeCurrentInstance();
        }
    }

    private static void fillInvocationCtx(InvocationContext invocationCtx, BaseRequest baseRequest) {

        invocationCtx.setCalleeIp(baseRequest.getCalleeIp());

        invocationCtx.setCalleePort(baseRequest.getCalleePort());

        invocationCtx.setCallerIp(baseRequest.getCallerIp());

        invocationCtx.setCallerFrom(baseRequest.getCallerFrom());

        invocationCtx.setCustomerName(baseRequest.getCustomerName());

        invocationCtx.setCustomerId(baseRequest.getCustomerId());

        invocationCtx.setOperatorId(baseRequest.getOperatorId());

        invocationCtx.setOperatorName(baseRequest.getOperatorName());

    }


}
