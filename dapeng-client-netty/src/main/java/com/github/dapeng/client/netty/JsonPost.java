package com.github.dapeng.client.netty;

import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.SoaConnectionPool;
import com.github.dapeng.core.SoaConnectionPoolFactory;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.json.JsonSerializer;
import com.github.dapeng.util.DumpUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;


/**
 * Created by tangliu on 2016/4/13.
 */
public class JsonPost {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPost.class);

    private boolean doNotThrowError = false;

    private SoaConnectionPool pool;

    public JsonPost(String serviceName, String version) {
        ServiceLoader<SoaConnectionPoolFactory> factories = ServiceLoader.load(SoaConnectionPoolFactory.class);
        for (SoaConnectionPoolFactory factory : factories) {
            this.pool = factory.getPool();
            break;
        }
        this.pool.registerClientInfo(serviceName, version);
    }

    public JsonPost(String serviceName, String version, boolean doNotThrowError) {
        this(serviceName, version);
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
    public String callServiceMethod(InvocationContext invocationContext,
                                    String jsonParameter, Service service) throws Exception {

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


        JsonSerializer jsonEncoder = new JsonSerializer(service, method, method.request);
        JsonSerializer jsonDecoder = new JsonSerializer(service, method, method.response);

        final long beginTime = System.currentTimeMillis();

        LOGGER.info("soa-request: " + jsonParameter);

        String escapedJson = StringEscapeUtils.escapeEcmaScript(jsonParameter);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("EscapedJson:" + escapedJson);
        }

        String jsonResponse = post(invocationContext.getServiceName(), invocationContext.getVersionName(),
                method.name, escapedJson, jsonEncoder, jsonDecoder);

        LOGGER.info("soa-response: " + DumpUtil.formatToString(jsonResponse) + (System.currentTimeMillis() - beginTime) + "ms");

        return jsonResponse;
    }


    /**
     * 构建客户端，发送和接收请求
     *
     * @return
     */
    private String post(String serviceName, String version, String method, String requestJson, JsonSerializer jsonEncoder, JsonSerializer jsonDecoder) throws Exception {

        String jsonResponse = "{}" ;

        try {
            String result = this.pool.send(serviceName, version, method, requestJson, jsonEncoder, jsonDecoder);

            jsonResponse = result.equals("{}")?"{\"status\":1}":result.substring(0,result.lastIndexOf('}')) + ",\"status\":1}";

        } catch (SoaException e) {

            LOGGER.error(e.getMsg(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\", \"status\":0}", e.getCode(), e.getMsg(), "{}");
            } else {
                throw e;
            }

        }  catch (Exception e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\", \"status\":0}", "9999", "系统繁忙，请稍后再试[9999]！", "{}");
            } else {
                throw e;
            }

        }

        return jsonResponse;
    }
}