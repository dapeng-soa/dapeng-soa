/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

        final int tagBase; // maybe < 0

        /**
         * 数组方式， 更高效，需要注意，
         * 1. 不连续key很大的情况， 例如来了个tag为65546的field
         * 2. 有些结构体定时的时候没填tag， 结果生成元数据的时候就变成了负数
         *
         * 所以目前采用Map的方式
         */
        private final Map<Short, Field> fieldMapByTag;
        private final Field[] fieldArrayByTag;

        public Struct getStruct() {
            return struct;
        }

        public OptimizedStruct(Struct struct) {
            this.struct = struct;

            int tagBase = 0;
            int maxTag = 0;

            for (Field f : struct.fields) {
                this.fieldMap.put(f.name, f);
                if(f.tag < tagBase) tagBase = f.tag;
                if(f.tag > maxTag) maxTag = f.tag;
            }

            this.tagBase = tagBase;
            Field[] array = null;
            Map<Short, Field> map = null;
            if(maxTag - tagBase + 1 <= 256) {
                array = new Field[maxTag - tagBase + 1];
                for(Field f: struct.fields) {
                    array[f.tag - tagBase] = f;
                }
            }
            else {
                map = new HashMap<>();
                for(Field f: struct.fields) {
                    map.put((short)f.tag, f);
                }
            }
            this.fieldArrayByTag = array;
            this.fieldMapByTag = map;
        }

        public Field get(short tag) {
            if(fieldArrayByTag != null && tag >= tagBase && tag - tagBase < fieldArrayByTag.length)
                return fieldArrayByTag[tag - tagBase];
            else return fieldMapByTag.get(tag);
        }
    }
}
