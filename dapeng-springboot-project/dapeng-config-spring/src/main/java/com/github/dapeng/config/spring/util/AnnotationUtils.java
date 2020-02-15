package com.github.dapeng.config.spring.util;

import com.github.dapeng.config.annotation.DapengService;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.String.valueOf;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.util.CollectionUtils.arrayToList;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.StringUtils.trimWhitespace;

public class AnnotationUtils {

    public static <T> T[] of(T... values) {
        return values;
    }

    public static String resolveInterfaceName(DapengService service, Class<?> defaultInterfaceClass)
            throws IllegalStateException {

        String interfaceName;
        if (StringUtils.hasText(service.interfaceName())) {
            interfaceName = service.interfaceName();
        } else if (!void.class.equals(service.interfaceClass())) {
            interfaceName = service.interfaceClass().getName();
        } else if (defaultInterfaceClass.isInterface()) {
            interfaceName = defaultInterfaceClass.getName();
        } else {
            throw new IllegalStateException(
                    "The @Service undefined interfaceClass or interfaceName, and the type "
                            + defaultInterfaceClass.getName() + " is not a interface.");
        }

        return interfaceName;

    }

    public static Map<String, Object> getAttributes(Annotation annotation, PropertyResolver propertyResolver,
                                                    boolean ignoreDefaultValue, String... ignoreAttributeNames) {

        Set<String> ignoreAttributeNamesSet = new HashSet<String>(arrayToList(ignoreAttributeNames));

        Map<String, Object> attributes = getAnnotationAttributes(annotation);

        Map<String, Object> actualAttributes = new LinkedHashMap<String, Object>();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {

            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();

            // ignore default attribute value
            if (ignoreDefaultValue && nullSafeEquals(attributeValue, getDefaultValue(annotation, attributeName))) {
                continue;
            }

            // ignore attribute name
            if (ignoreAttributeNamesSet.contains(attributeName)) {
                continue;
            }

            /**
             * @since 2.6.6
             * ignore annotation member
             */
            if (attributeValue.getClass().isAnnotation()) {
                continue;
            }
            if (attributeValue.getClass().isArray() && attributeValue.getClass().getComponentType().isAnnotation()) {
                continue;
            }


            if (attributeValue instanceof String) {
                attributeValue = resolvePlaceholders(valueOf(attributeValue), propertyResolver);
            } else if (attributeValue instanceof String[]) {
                String[] values = (String[]) attributeValue;
                for (int i = 0; i < values.length; i++) {
                    values[i] = resolvePlaceholders(values[i], propertyResolver);
                }
                attributeValue = values;
            }
            actualAttributes.put(attributeName, attributeValue);
        }
        return actualAttributes;
    }

    private static String resolvePlaceholders(String attributeValue, PropertyResolver propertyResolver) {
        String resolvedValue = attributeValue;
        if (propertyResolver != null) {
            resolvedValue = propertyResolver.resolvePlaceholders(resolvedValue);
            resolvedValue = trimWhitespace(resolvedValue);
        }
        return resolvedValue;
    }


}
