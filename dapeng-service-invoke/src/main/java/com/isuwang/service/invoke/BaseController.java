package com.github.dapeng.service.invoke;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.SoaSystemEnvProperties;
import com.github.dapeng.service.invoke.entity.BaseRequest;
import com.github.dapeng.registry.RegistryAgent;
import com.github.dapeng.registry.RegistryAgentProxy;
import com.github.dapeng.remoting.BaseClient;
import com.github.dapeng.remoting.fake.json.JSONPost;
import com.github.dapeng.remoting.filter.LoadBalanceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Created by lihuimin on 2017/12/6.
 */
public class BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseController.class);

    private static JSONPost jsonPost;

    static {
        if (!SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local")) {
            try {

                ServiceLoader<RegistryAgent> registryAgentLoader = ServiceLoader.load(RegistryAgent.class, BaseClient.class.getClassLoader());
                for (RegistryAgent registryAgent : registryAgentLoader) {
                    RegistryAgentProxy.setCurrentInstance(RegistryAgentProxy.Type.Client, registryAgent);
                    RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Client).start();
                    ApiServices apiServices = new ApiServices();
                    new ZookeeperWatcher(true,apiServices).init();
                }
            } catch (Exception e) {
                LOGGER.error("Load registry error", e);
            }
        } else {
            LOGGER.info("soa remoting mode is {},client not load registry", SoaSystemEnvProperties.SOA_REMOTING_MODE);
        }

    }


    public static String invoke(BaseRequest baseRequest) {
        if (baseRequest.getServiceName() == null || baseRequest.getVersionName() == null || baseRequest.getMethodName() == null) {
            return String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "Err-Core-098", "serviceName、versionName、methodName信息不能为null", "{}");
        }

        JsonObject jsonObjectParameter = new JsonObject();
        if (baseRequest.getJsonParameter() != null) {
            jsonObjectParameter = new JsonParser().parse(baseRequest.getJsonParameter()).getAsJsonObject();
        }
        com.github.dapeng.core.metadata.Service service = ApiServices.getService(baseRequest.getServiceName(), baseRequest.getVersionName());

        String callerInfo = LoadBalanceFilter.getCallerInfo(baseRequest.getServiceName(), baseRequest.getVersionName(), baseRequest.getVersionName());

        SoaHeader header = new SoaHeader();
        header.setServiceName(baseRequest.getServiceName());
        header.setVersionName(baseRequest.getVersionName());
        header.setMethodName(baseRequest.getMethodName());

        String parameter = jsonObjectParameter.toString();

        if (callerInfo == null && SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local")) {
            jsonPost = new JSONPost(SoaSystemEnvProperties.SOA_SERVICE_IP, SoaSystemEnvProperties.SOA_SERVICE_PORT, true);
        } else if (callerInfo != null) {
            String[] infos = callerInfo.split(":");
            jsonPost = new JSONPost(infos[0], Integer.valueOf(infos[1]), true);
        } else {
            return String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "Err-Core-098", "无可用的服务实例", "{}");
        }
        try {
            return jsonPost.callServiceMethod(header, parameter, service);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}
