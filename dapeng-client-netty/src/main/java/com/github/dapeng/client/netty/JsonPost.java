package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnectionPool;
import com.github.dapeng.core.SoaConnectionPoolFactory;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.json.JsonSerializer;
import com.github.dapeng.util.DumpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;


/**
 * @author tangliu
 * @date 2016/4/13
 */
public class JsonPost {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPost.class);

    private boolean doNotThrowError = false;

    private final static SoaConnectionPoolFactory factory = ServiceLoader.load(SoaConnectionPoolFactory.class, JsonPost.class.getClassLoader()).iterator().next();


    private SoaConnectionPool pool;
    private final SoaConnectionPool.ClientInfo clientInfo;
    private final String methodName;

    public JsonPost(final String serviceName, final String version, final String methodName) {
        this.methodName = methodName;
        this.pool = factory.getPool();
        this.clientInfo = this.pool.registerClientInfo(serviceName, version);
    }

    public JsonPost(final String serviceName, final String version, final String methodName, boolean doNotThrowError) {
        this(serviceName, version, methodName);
        this.doNotThrowError = doNotThrowError;
    }

    /**
     * 调用远程服务
     *
     * @param jsonParameter
     * @param service
     * @return
     * @throws Exception
     */
    public String callServiceMethod(final String jsonParameter,
                                    final Service service) throws Exception {
        List<Method> targetMethods = service.getMethods().stream().filter(element ->
                element.name.equals(methodName))
                .collect(Collectors.toList());

        if (targetMethods.isEmpty()) {
            return "method:" + methodName + " for service:"
                    + clientInfo.serviceName + " not found";
        }

        Method method = targetMethods.get(0);


        JsonSerializer jsonEncoder = new JsonSerializer(service, method, clientInfo.version, method.request);
        JsonSerializer jsonDecoder = new JsonSerializer(service, method, clientInfo.version, method.response);

        final long beginTime = System.currentTimeMillis();

        LOGGER.info("soa-request: service:[" + service.namespace + "." + service.name
                + ":" + service.meta.version + "], method:" + methodName + ", param:"
                + jsonParameter);

        String jsonResponse = post(clientInfo.serviceName, clientInfo.version,
                methodName, jsonParameter, jsonEncoder, jsonDecoder);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("soa-response: " + jsonResponse + " cost:" + (System.currentTimeMillis() - beginTime) + "ms");
        } else {
            LOGGER.info("soa-response: " + DumpUtil.formatToString(jsonResponse) + (System.currentTimeMillis() - beginTime) + "ms");
        }

        return jsonResponse;
    }


    /**
     * 构建客户端，发送和接收请求
     *
     * @return
     */
    private String post(String serviceName, String version, String method, String requestJson, JsonSerializer jsonEncoder, JsonSerializer jsonDecoder) throws Exception {

        String jsonResponse = "{}";

        try {
            String result = this.pool.send(serviceName, version, method, requestJson, jsonEncoder, jsonDecoder);

            jsonResponse = result.equals("{}") ? "{\"status\":1}" : result.substring(0, result.lastIndexOf('}')) + ",\"status\":1}";

        } catch (SoaException e) {

            LOGGER.error(e.getMsg(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\", \"status\":0}", e.getCode(), e.getMsg(), "{}");
            } else {
                throw e;
            }

        } catch (Exception e) {

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