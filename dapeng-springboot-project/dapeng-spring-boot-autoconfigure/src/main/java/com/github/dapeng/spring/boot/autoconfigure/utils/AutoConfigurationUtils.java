package com.github.dapeng.spring.boot.autoconfigure.utils;

import org.springframework.core.env.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutoConfigurationUtils {

    public static final String DAPENG_PREFIX = "dapeng.autoconfigure";

    public static final String DAPENG_SCAN_PREFIX = "dapeng.scan";

    public static final String BASE_PACKAGES_PROPERTY_NAME = "base-packages";

    public static final String BASE_PACKAGES_PROPERTY_RESOLVER_BEAN_NAME = "dapengScanBasePackagesPropertyResolver";

    public static Map<String, Object> getSubProperties(PropertySources propertySources, String prefix) {
        return getSubProperties(propertySources, new PropertySourcesPropertyResolver(propertySources), prefix);
    }

    public static Map<String, Object> getSubProperties(PropertySources propertySources, PropertyResolver propertyResolver, String prefix) {
        Map<String, Object> subProperties = new LinkedHashMap();
        String normalizedPrefix = normalizePrefix(prefix);
        Iterator iterator = propertySources.iterator();

        while (true) {
            PropertySource source;
            do {
                if (!iterator.hasNext()) {
                    return Collections.unmodifiableMap(subProperties);
                }

                source = (PropertySource) iterator.next();
            } while (!(source instanceof EnumerablePropertySource));

            String[] var7 = ((EnumerablePropertySource) source).getPropertyNames();
            int var8 = var7.length;

            for (int var9 = 0; var9 < var8; ++var9) {
                String name = var7[var9];
                if (!subProperties.containsKey(name) && name.startsWith(normalizedPrefix)) {
                    String subName = name.substring(normalizedPrefix.length());
                    if (!subProperties.containsKey(subName)) {
                        Object value = source.getProperty(name);
                        if (value instanceof String) {
                            value = propertyResolver.resolvePlaceholders((String) value);
                        }

                        subProperties.put(subName, value);
                    }
                }
            }
        }
    }

    public static String normalizePrefix(String prefix) {
        return prefix.endsWith(".") ? prefix : prefix + ".";
    }
}
