package com.github.dapeng.json;

import com.github.dapeng.core.metadata.*;

import java.util.ArrayList;
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
                optimizedStructs.put(method.request.namespace + "." + method.request.name, new OptimizedStruct(method.request));

                optimizedStructs.put(method.request.name + ".body", wrapperReq(method));
                optimizedStructs.put(method.response.namespace + "." + method.response.name, new OptimizedStruct(method.response));
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

        private OptimizedStruct wrapperReq(Method method) {
            Struct reqWrapperStruct = new Struct();
            reqWrapperStruct.name = "body";
            reqWrapperStruct.namespace = method.name;
            reqWrapperStruct.fields = new ArrayList<>(2);
            Field reqField = new Field();
            reqField.tag = 0;
            DataType reqDataType = new DataType();
            reqDataType.kind = DataType.KIND.STRUCT;
            reqDataType.qualifiedName = method.request.name;
            reqField.dataType = reqDataType;
            reqWrapperStruct.fields.add(reqField);

            return new OptimizedStruct(reqWrapperStruct);
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
         *
         * 所以目前采用Map的方式
         */
        final Map<Short, Field> fieldMapByTag = new HashMap<>(128);

        public Struct getStruct() {
            return struct;
        }

        public OptimizedStruct(Struct struct) {
            this.struct = struct;
            for (Field f : struct.fields) {
                this.fieldMap.put(f.name, f);
                this.fieldMapByTag.put((short)f.tag, f);
            }
        }
    }
}
