package com.github.dapeng.service.invoke;

import com.google.common.collect.TreeMultimap;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.metadata.*;
import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.remoting.fake.metadata.MetadataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Tony_PC on 2016/6/20.
 */
public class ApiServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiServices.class);

    private static Map<String, Service> services = new TreeMap<>();

    private static Map<String, Service> fullNameService = new TreeMap<>();

    public static TreeMultimap<String, String> urlMappings = TreeMultimap.create();

    private static ZookeeperWatcher zookeeperWatcher;

    public static void init() {
        reloadServices();
    }

    public ApiServices(){
        init();
    }

    public static void reloadServices() {

       final Map<String, Service> services = new TreeMap<>();
        urlMappings.clear();

        Map<String, List<ServiceInfo>> servicesInfo = zookeeperWatcher.getAvailableServices();

        Set<String> serviceKeys = servicesInfo.keySet();

        for (String key : serviceKeys) {
            String serviceName = key;
            List<ServiceInfo> serviceInfoList = servicesInfo.get(key);
            for (ServiceInfo serviceInfo : serviceInfoList) {
                String version = serviceInfo.getVersionName();
                String metadata = "";
                try {
                    metadata = new MetadataClient(serviceName, version).getServiceMetadata();
                    if (metadata != null) {
                        try (StringReader reader = new StringReader(metadata)) {
                            Service serviceData = JAXB.unmarshal(reader, Service.class);
                            String serviceKey = getKey(serviceData);
                            if (!services.containsKey(serviceKey)) {
                                services.put(serviceKey, serviceData);
                                String fullNameKey = getFullNameKey(serviceData);
                                fullNameService.put(fullNameKey, serviceData);
                                loadResource(serviceData, services);
                            }
                        } catch (Exception e) {
                            LOGGER.error("JAXB 解析Service 出错");
                        }
                    }
                } catch (SoaException e) {
                    LOGGER.error("生成SERVICE出错", e);
                }
            }
        }
        ApiServices.services = services;
        LOGGER.info("size of urlMapping: " + urlMappings.size());
    }

    public void destory() {
        services.clear();
    }

    public static void loadResource(Service service, Map<String, Service> services) {

        //将service和service中的方法、结构体、枚举和字段名分别设置对应的url，以方便搜索
        urlMappings.put(service.getName(), "api/service/" + service.name + "/" + service.meta.version + ".htm");
        List<Method> methods = service.getMethods();
        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            urlMappings.put(method.name, "api/method/" + service.name + "/" + service.meta.version + "/" + method.name + ".htm");
        }

        List<Struct> structs = service.getStructDefinitions();
        for (int i = 0; i < structs.size(); i++) {
            Struct struct = structs.get(i);
            urlMappings.put(struct.name, "api/struct/" + service.name + "/" + service.meta.version + "/" + struct.namespace + "." + struct.name + ".htm");

            List<Field> fields = struct.getFields();
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get(j);
                urlMappings.put(field.name, "api/struct/" + service.name + "/" + service.meta.version + "/" + struct.namespace + "." + struct.name + ".htm");
            }
        }

        List<TEnum> tEnums = service.getEnumDefinitions();
        for (int i = 0; i < tEnums.size(); i++) {
            TEnum tEnum = tEnums.get(i);
            urlMappings.put(tEnum.name, "api/enum/" + service.name + "/" + service.meta.version + "/" + tEnum.namespace + "." + tEnum.name + ".htm");
        }

    }

    public static Service getService(String name, String version) {

        if (name.contains("."))
            return fullNameService.get(getKey(name, version));
        else
            return services.get(getKey(name, version));
    }

    private static String getKey(Service service) {
        return getKey(service.getName(), service.getMeta().version);
    }

    private static String getFullNameKey(Service service) {
        return getKey(service.getNamespace() + "." + service.getName(), service.getMeta().version);
    }

    private static String getKey(String name, String version) {
        return name + ":" + version;
    }

    public Map<String, Service> getServices() {
        return services;
    }

    public ZookeeperWatcher getZookeeperWatcher() {
        return zookeeperWatcher;
    }

    public void setZookeeperWatcher(ZookeeperWatcher zookeeperWatcher) {
        this.zookeeperWatcher = zookeeperWatcher;
    }

}
