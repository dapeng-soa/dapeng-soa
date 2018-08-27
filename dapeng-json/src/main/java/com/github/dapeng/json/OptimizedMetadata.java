package com.github.dapeng.json;

import com.github.dapeng.core.metadata.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜索优化的元数据结构
 *
 * @author zxwang
 */
public class OptimizedMetadata {

    public static class OptimizedService {
        final Service service;

        final Map<String, Method> methodMap = new HashMap<>(128);

        final Map<String, OptimizedStruct> optimizedStructs = new HashMap<>(1024);
        final Map<String, TEnum> enumMap = new HashMap<>(128);

        public OptimizedService(Service service) {
            this.service = service;
            for (Struct struct : service.structDefinitions) {
                optimizedStructs.put(struct.namespace + "." + struct.name, new OptimizedStruct(struct));
            }
            for (TEnum tEnum : service.enumDefinitions) {
                enumMap.put(tEnum.namespace + "." + tEnum.name, tEnum);
            }
            for (Method method: service.methods) {
                methodMap.put(method.name, method);
                optimizedStructs.put(service.name + "." + method.request.name, new OptimizedStruct(method.request));
                optimizedStructs.put(service.name + "." + method.response.name, new OptimizedStruct(method.response));
            }
        }

        public Service getService() {
            return service;
        }

        public Map<String, Method> getMethodMap() {
            return Collections.unmodifiableMap(methodMap);
        }

        public Map<String, OptimizedStruct> getOptimizedStructs() {
            return Collections.unmodifiableMap(optimizedStructs);
        }

        public Map<String, TEnum> getEnumMap() {
            return Collections.unmodifiableMap(enumMap);
        }
    }

    public static class OptimizedStruct {
        final Struct struct;

        /**
         *
         */
        final Map<String, Field> fieldMap = new HashMap<>(128);
        /**
         * 数组方式， 更高效，需要注意，
         * 1. 不连续key很大的情况， 例如来了个tag为65546的field
         * 2. 有些结构体定时的时候没填tag， 结果生成元数据的时候就变成了负数
         */
//        final Field[] fields;
        final Map<Integer, Field> fieldMapByTag = new HashMap<>(128);

        public Struct getStruct() {
            return struct;
        }

        public OptimizedStruct(Struct struct) {
            this.struct = struct;
            for (Field f : struct.fields) {
                this.fieldMap.put(f.name, f);
                this.fieldMapByTag.put(f.tag, f);
            }
        }
    }
}
