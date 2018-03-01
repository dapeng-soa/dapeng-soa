package com.github.dapeng.service.invoke;

import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.metadata.MetadataClient;
import com.google.common.collect.TreeMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by lihuimin on 2017/12/6.
 */
public class ApiServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiServices.class);

    private static Map<String, Service> services = new TreeMap<>();

    private static Map<String, Service> fullNameService = new TreeMap<>();

    public static TreeMultimap<String, String> urlMappings = TreeMultimap.create();

    public static void loadServices(String serviceName,String version) {
        urlMappings.clear();

        String metadata = "";
        try {
            //init service,no need to set params
            InvocationContextImpl.Factory.createNewInstance();

            metadata = new MetadataClient(serviceName,version)
                    .getServiceMetadata();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            InvocationContextImpl.Factory.removeCurrentInstance();
        }

        if (metadata != null) {
            try (StringReader reader = new StringReader(metadata)) {
                Service serviceData = JAXB.unmarshal(reader, Service.class);
                loadResource(serviceData);
            } catch (Exception e) {
                LOGGER.error("生成SERVICE出错", e);
            }
        }


        LOGGER.info("size of urlMapping: " + urlMappings.size());

    }

    public void destory() {
        services.clear();
    }

    public static void loadResource(Service service) {

        String key = getKey(service);
        services.put(key, service);

        String fullNameKey = getFullNameKey(service);
        fullNameService.put(fullNameKey, service);

        //将service和service中的方法、结构体、枚举和字段名分别设置对应的url，以方便搜索
        urlMappings.put(service.getName(), "api/service/" + service.name + "/" + service.meta.version + ".htm");

        service.getMethods().forEach(method -> {
            urlMappings.put(method.name, "api/method/" + service.name + "/"
                    + service.meta.version + "/" + method.name + ".htm");
        });

        service.getStructDefinitions().forEach(struct -> {
            urlMappings.put(struct.name, "api/struct/" + service.name + "/"
                    + service.meta.version + "/" + struct.namespace + "."
                    + struct.name + ".htm");

            struct.getFields().forEach(field -> {
                urlMappings.put(field.name, "api/struct/" + service.name + "/"
                        + service.meta.version + "/" + struct.namespace + "."
                        + struct.name + ".htm");
            });
        });

        service.getEnumDefinitions().forEach(tEnum -> {
            urlMappings.put(tEnum.name, "api/enum/" + service.name + "/"
                    + service.meta.version + "/" + tEnum.namespace + "."
                    + tEnum.name + ".htm");
        });
    }

    public static Service getServiceFromCache(String name, String version) {
        if (name.contains("."))
            return fullNameService.get(getKey(name, version));
        else
            return services.get(getKey(name, version));
    }

    public static Service getService(String name, String version) {

        Service service = getServiceFromCache(name,version);
        if (service == null){
            loadServices(name,version);
        }
        return getServiceFromCache(name,version);
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


}
