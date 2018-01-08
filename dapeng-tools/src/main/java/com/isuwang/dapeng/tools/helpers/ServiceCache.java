package com.github.dapeng.tools.helpers;

/**
 * @author Shadow
 * @date
 */

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.remoting.fake.metadata.MetadataClient;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by tangliu on 2016/6/8.
 */
public class ServiceCache {

    private static Map<String, Service> serviceCache = new TreeMap<String, Service>();


    public static Service getService(String serviceName, String versionName) {

        String key = getKey(serviceName, versionName);
        if (serviceCache.containsKey(key)) {
            return serviceCache.get(key);
        }

        String metadata = "";

        try {
            metadata = new MetadataClient(serviceName, versionName).getServiceMetadata();
        } catch (SoaException e) {
            System.out.println(e.getMsg());
        }

        if (metadata != null && !"".equals(metadata)) {
            try {
                StringReader reader = new StringReader(metadata);
                Service service = JAXB.unmarshal(reader, Service.class);
                serviceCache.put(key, service);
            } catch (Exception e) {
                System.err.println("生成service出错" + e.getMessage());
            }
        }

        return serviceCache.get(key);
    }

    private static String getKey(String service, String version) {
        return service + ":" + version;
    }

}