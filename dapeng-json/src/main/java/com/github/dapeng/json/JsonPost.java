package com.github.dapeng.json;

import com.github.dapeng.client.netty.SoaConnectionImpl;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.util.SoaSystemEnvProperties;
import com.github.dapeng.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tangliu on 2016/4/13.
 */
public class JsonPost {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPost.class);

    private String host = "127.0.0.1";

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
            jsonParameter = "{}";
        }

//        ObjectMapper objectMapper = new ObjectMapper();
//        StringWriter out = new StringWriter();
//
//        @SuppressWarnings("unchecked")
//        Map<String, Map<String, Object>> params = objectMapper.readValue(jsonParameter, Map.class);
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("serviceName", invocationContext.getServiceName());
//        map.put("version", invocationContext.getVersionName());
//        map.put("methodName", invocationContext.getMethodName());
//        map.put("params", params);
//
//        objectMapper.writeValue(out, map);
//
//        //发起请求
//        final DataInfo request = new DataInfo();
//        request.setConsumesType("JSON");
//        request.setConsumesValue(out.toString());
//        request.setServiceName(invocationContext.getServiceName());
//        request.setVersion(invocationContext.getVersionName());
//        request.setMethodName(invocationContext.getMethodName());

        Method method = null;
        for (Method _method: service.getMethods()) {
            if (_method.getName().equals(invocationContext.getMethodName())) {
                method = _method;
                break;
            }
        }

        JsonSerializer jsonEncoder = new JsonSerializer(service,method,method.request);
        JsonSerializer jsonDecoder = new JsonSerializer(service,method,method.response);

        final long beginTime = System.currentTimeMillis();

        LOGGER.info("soa-request: {}", jsonParameter);

        String jsonResponse = post(jsonParameter, jsonEncoder, jsonDecoder);

        LOGGER.info("soa-response: {} {}ms", jsonResponse, System.currentTimeMillis() - beginTime);

        return jsonResponse;
    }


    /**
     * 构建客户端，发送和接收请求
     *
     * @return
     */
    private String post(String requestJson, JsonSerializer jsonEncoder, JsonSerializer jsonDecoder) throws Exception {

        String jsonResponse = "{}";

        TSoaTransport inputSoaTransport = null;
        TSoaTransport outputSoaTransport = null;

        try {
            //TODO: need serialize jsonMap to RequestObj

            Object result = new SoaConnectionImpl(host,port).sendJson(requestJson,jsonEncoder,jsonEncoder);

            jsonResponse = (String)result;

        } catch (SoaException e) {

            LOGGER.error(e.getMsg());
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", e.getCode(), e.getMsg(), "{}");
            } else {
                throw e;
            }

        } catch (TException e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", e.getMessage(), "{}");
            } else {
                throw e;
            }

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", "系统繁忙，请稍后再试[9999]！", "{}");
            }
            else {
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
