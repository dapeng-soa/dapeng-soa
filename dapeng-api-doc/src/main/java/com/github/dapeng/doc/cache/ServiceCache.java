package com.github.dapeng.doc.cache;


import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.metadata.*;
import com.github.dapeng.json.OptimizedMetadata;
import com.google.common.collect.TreeMultimap;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.ServiceInfo;
import com.github.dapeng.metadata.MetadataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Service Cache
 *
 * @author craneding
 * @date 15/4/26
 */
public class ServiceCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCache.class);

    private static Map<String, OptimizedMetadata.OptimizedService> services = new TreeMap<>();

    private static Map<String, OptimizedMetadata.OptimizedService> fullNameService = new TreeMap<>();

    public static TreeMultimap<String, String> urlMappings = TreeMultimap.create();

    public void init() {
        System.out.println("------------------------- Initialize serviceCache......");

        System.out.println("--------------------Container: " + ContainerFactory.getContainer());
        System.out.println("--------------------Applications: " + ContainerFactory.getContainer().getApplications());
        System.out.println("--------------------Filters: " + ContainerFactory.getContainer().getFilters());

        List<Application> applications = ContainerFactory.getContainer().getApplications();
        applications.forEach(i -> loadServices(i));
    }

    private void unloadServices(Application application) {
        //Some specific logic here
    }


    private void loadServices(Application application) {

        urlMappings.clear();

        List<ServiceInfo> serviceInfos = application.getServiceInfos();
        serviceInfos.forEach(serviceInfo -> {
            String metadata = "";
            try {
                //init service,no need to set params
                InvocationContext invocationContext = InvocationContextImpl.Factory.createNewInstance();
                invocationContext.timeout(5000);
                metadata = new MetadataClient(serviceInfo.serviceName, serviceInfo.version)
                        .getServiceMetadata();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                InvocationContextImpl.Factory.removeCurrentInstance();
            }

            if (metadata != null) {
                try (StringReader reader = new StringReader(metadata)) {
                    Service serviceData = JAXB.unmarshal(reader, Service.class);

                    //替换为注册的版本号
                    serviceData.getMeta().setVersion(serviceInfo.version);

                    OptimizedMetadata.OptimizedService optimizedService = new OptimizedMetadata.OptimizedService(serviceData);

                    Map<String, OptimizedMetadata.OptimizedService> services = loadResource(optimizedService);
                    ServiceCache.services.putAll(services);
                } catch (Exception e) {
                    LOGGER.error("生成SERVICE[" + serviceInfo.serviceName + "]出错, metaData:\n" + metadata, e);
                }
            }
        });

        LOGGER.info("size of urlMapping: " + urlMappings.size());
    }

    public void destory() {
        services.clear();
    }

    public Map<String, OptimizedMetadata.OptimizedService> loadResource(OptimizedMetadata.OptimizedService optimizedService) {

        final Map<String, OptimizedMetadata.OptimizedService> services = new TreeMap<>();

        Service service = optimizedService.getService();

        String key = getKey(service);
        services.put(key, optimizedService);

        String fullNameKey = getFullNameKey(service);
        fullNameService.put(fullNameKey, optimizedService);


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

        return services;
    }

    public OptimizedMetadata.OptimizedService getService(String name, String version) {

        if (name.contains(".")) {
            return fullNameService.get(getKey(name, version));
        } else {
            return services.get(getKey(name, version));
        }
    }

    private String getKey(Service service) {
        return getKey(service.getName(), service.getMeta().version);
    }

    private String getFullNameKey(Service service) {
        return getKey(service.getNamespace() + "." + service.getName(), service.getMeta().version);
    }

    private String getKey(String name, String version) {
        return name + ":" + version;
    }

    public Map<String, OptimizedMetadata.OptimizedService> getServices() {
        return services;
    }

}
