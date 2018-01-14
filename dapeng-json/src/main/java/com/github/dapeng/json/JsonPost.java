package com.github.dapeng.json;

import com.github.dapeng.client.netty.SoaConnectionImpl;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by tangliu on 2016/4/13.
 */
public class JsonPost {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPost.class);

    private String host = "127.0.0.1" ;

    private Integer port = SoaSystemEnvProperties.SOA_CONTAINER_PORT;

    private boolean doNotThrowError = false;

    public JsonPost(String host, Integer port, boolean doNotThrowError) {
        this.host = host;
        this.port = port;
        this.doNotThrowError = doNotThrowError;
    }

    /**
     * 调用远程服务
     *
     * @param invocationContext
     * @param jsonParameter
     * @param service
     * @return
     * @throws Exception
     */
    public String callServiceMethod(InvocationContext invocationContext, String jsonParameter, Service service) throws Exception {

        if (null == jsonParameter || "".equals(jsonParameter.trim())) {
            jsonParameter = "{}" ;
        }

        List<Method> targetMethods = service.getMethods().stream().filter(element ->
                element.name.equals(invocationContext.getMethodName()))
                .collect(Collectors.toList());

        if (targetMethods.isEmpty()) {
            return "method:" + invocationContext.getMethodName() + " for service:"
                    + invocationContext.getServiceName() + " not found" ;
        }

        Method method = targetMethods.get(0);


        JsonSerializer jsonEncoder = new JsonSerializer(service, method, method.request, jsonParameter);
        JsonSerializer jsonDecoder = new JsonSerializer(service, method, method.response);

        final long beginTime = System.currentTimeMillis();

        LOGGER.info("soa-request: {}", jsonParameter);

        String jsonResponse = post(invocationContext.getServiceName(), invocationContext.getVersionName(), method.name,jsonParameter, jsonEncoder, jsonDecoder);

        LOGGER.info("soa-response: {} {}ms", jsonResponse, System.currentTimeMillis() - beginTime);

        return jsonResponse;
    }


    /**
     * 构建客户端，发送和接收请求
     *
     * @return
     */
    private String post(String serviceName, String version, String method, String requestJson, JsonSerializer jsonEncoder, JsonSerializer jsonDecoder) throws Exception {

        String jsonResponse = "{}" ;

        TSoaTransport inputSoaTransport = null;
        TSoaTransport outputSoaTransport = null;

        try {
            Object result = new SoaJsonConnectionImpl(host, port).send(serviceName, version, method, requestJson, jsonEncoder, jsonDecoder);

            jsonResponse = (String) result;

        } catch (SoaException e) {

            LOGGER.error(e.getMsg());
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", e.getCode(), e.getMsg(), "{}");
            } else {
                throw e;
            }

        }  catch (Exception e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", "系统繁忙，请稍后再试[9999]！", "{}");
            } else {
                throw e;
            }

        } finally {
            if (outputSoaTransport != null) {
                outputSoaTransport.close();
            }

            if (inputSoaTransport != null) {
                inputSoaTransport.close();
            }
        }

        return jsonResponse;
    }
}
