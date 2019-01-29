package com.github.dapeng.client.netty;

import com.github.dapeng.core.*;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.json.JsonSerializer;
import com.github.dapeng.json.OptimizedMetadata;
import com.github.dapeng.util.DumpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.github.dapeng.util.InvocationContextUtils.capsuleContext;


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
     * @param optimizedService
     * @return
     * @throws Exception
     */
    public String callServiceMethod(final String jsonParameter,
                                    final OptimizedMetadata.OptimizedService optimizedService) throws Exception {
        Method method = optimizedService.getMethodMap().get(methodName);

        if (method == null) {
            return String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"{}\", \"status\":0}",
                    SoaCode.NoMatchedMethod,
                    "method:" + methodName + " for service:" + clientInfo.serviceName + " not found");
        }

        try {
            InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
            capsuleContext((InvocationContextImpl)invocationContext, clientInfo.serviceName, clientInfo.version, methodName);
            String sessionTid = invocationContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0");

            String logLevel = invocationContext.cookie(SoaSystemEnvProperties.THREAD_LEVEL_KEY);

            if (logLevel != null) {
                MDC.put(SoaSystemEnvProperties.THREAD_LEVEL_KEY, logLevel);
            }

            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);

            OptimizedMetadata.OptimizedStruct req = optimizedService.getOptimizedStructs().get(method.request.namespace + "." + method.request.name);
            OptimizedMetadata.OptimizedStruct resp = optimizedService.getOptimizedStructs().get(method.response.namespace + "." + method.response.name);

            JsonSerializer jsonEncoder = new JsonSerializer(optimizedService, method, clientInfo.version, req);
            JsonSerializer jsonDecoder = new JsonSerializer(optimizedService, method, clientInfo.version, resp);

            final long beginTime = System.currentTimeMillis();

            Service origService = optimizedService.getService();
            LOGGER.info("soa-request: service:[" + origService.namespace + "." + origService.name
                    + ":" + origService.meta.version + "], method:" + methodName + ", param:"
                    + jsonParameter);

            String jsonResponse = post(clientInfo.serviceName, clientInfo.version,
                    methodName, jsonParameter, jsonEncoder, jsonDecoder);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("soa-response: " + jsonResponse + " cost:" + (System.currentTimeMillis() - beginTime) + "ms");
            } else {
                LOGGER.info("soa-response: " + DumpUtil.formatToString(jsonResponse) + " cost:" + (System.currentTimeMillis() - beginTime) + "ms");
            }

            return jsonResponse;
        } finally {
            MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            MDC.remove(SoaSystemEnvProperties.THREAD_LEVEL_KEY);
        }
    }


    /**
     * 构建客户端，发送和接收请求
     *
     * @return
     */
    private String post(String serviceName, String version, String method, String requestJson, JsonSerializer jsonEncoder, JsonSerializer jsonDecoder) throws Exception {

        String jsonResponse;
        String sessionTid = MDC.get(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
        try {
            String result = this.pool.send(serviceName, version, method, requestJson, jsonEncoder, jsonDecoder);
            jsonResponse = result.equals("{}") ? "{\"status\":1}" : result.substring(0, result.lastIndexOf('}')) + ",\"status\":1}";
            //MDC will be remove by client filter
            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);
        } catch (SoaException e) {
            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);
            if (DapengUtil.isDapengCoreException(e)) {
                LOGGER.error(e.getMsg(), e);
            } else {
                LOGGER.error(e.getMsg());
            }
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\", \"status\":0}", e.getCode(), e.getMsg(), "{}");
            } else {
                throw e;
            }

        } catch (Exception e) {
            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);
            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\", \"status\":0}", "9999", "系统繁忙，请稍后再试[9999]！", "{}");
            } else {
                throw e;
            }

        }

        return jsonResponse;
    }


    /**
     * 异步调用远程服务
     *
     * @param jsonParameter    json请求
     * @param optimizedService 服务元数据信息
     * @return
     * @throws Exception
     */
    public Future<String> callServiceMethodAsync(final String jsonParameter,
                                                 final OptimizedMetadata.OptimizedService optimizedService) throws Exception {
        Method method = optimizedService.getMethodMap().get(methodName);

        if (method == null) {

            String resp = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"{}\", \"status\":0}",
                    SoaCode.NoMatchedMethod,
                    "method:" + methodName + " for service:" + clientInfo.serviceName + " not found");
            return CompletableFuture.completedFuture(resp);
        }

        try {
            InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
            capsuleContext((InvocationContextImpl)invocationContext, clientInfo.serviceName, clientInfo.version, methodName);
            String sessionTid = invocationContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0");

            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);

            String logLevel = invocationContext.cookie(SoaSystemEnvProperties.THREAD_LEVEL_KEY);

            if (logLevel != null) {
                MDC.put(SoaSystemEnvProperties.THREAD_LEVEL_KEY, logLevel);
            }

            OptimizedMetadata.OptimizedStruct req = optimizedService.getOptimizedStructs().get(method.request.namespace + "." + method.request.name);
            OptimizedMetadata.OptimizedStruct resp = optimizedService.getOptimizedStructs().get(method.response.namespace + "." + method.response.name);

            JsonSerializer jsonEncoder = new JsonSerializer(optimizedService, method, clientInfo.version, req);
            JsonSerializer jsonDecoder = new JsonSerializer(optimizedService, method, clientInfo.version, resp);

            Service origService = optimizedService.getService();

            LOGGER.info("soa-request: service:[" + origService.namespace + "." + origService.name
                    + ":" + origService.meta.version + "], method:" + methodName + ", param:"
                    + jsonParameter);

            Future<String> jsonResponse = postAsync(clientInfo.serviceName, clientInfo.version,
                    methodName, jsonParameter, jsonEncoder, jsonDecoder);
            //MDC will be remove by client filter
            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);

            return jsonResponse;
        } finally {
            MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            MDC.remove(SoaSystemEnvProperties.THREAD_LEVEL_KEY);
        }


    }

    /**
     * 构建客户端，发送和接收异步请求
     *
     * @return
     */
    private Future<String> postAsync(String serviceName, String version, String method, String requestJson, JsonSerializer jsonEncoder, JsonSerializer jsonDecoder) throws Exception {
        Future<String> jsonResponse;
        try {
            jsonResponse = this.pool.sendAsync(serviceName, version, method, requestJson, jsonEncoder, jsonDecoder);
        } catch (SoaException e) {
            if (DapengUtil.isDapengCoreException(e)) {
                LOGGER.error(e.getMsg(), e);
            } else {
                LOGGER.error(e.getMsg());
            }
            if (doNotThrowError) {
                jsonResponse = CompletableFuture.completedFuture(String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\", \"status\":0}", e.getCode(), e.getMsg(), "{}"));
            } else {
                throw e;
            }

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError) {
                jsonResponse = CompletableFuture.completedFuture(String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\", \"status\":0}", "9999", "系统繁忙，请稍后再试[9999]！", "{}"));
            } else {
                throw e;
            }
        }
        return jsonResponse;
    }
}