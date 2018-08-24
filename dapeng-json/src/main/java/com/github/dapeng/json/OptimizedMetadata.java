package com.github.dapeng.json;

import com.github.dapeng.core.metadata.Field;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.core.metadata.Struct;
import com.github.dapeng.core.metadata.TEnum;

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
        }
    }

    public static class OptimizedStruct {
        final Struct struct;

        final Map<String, Field> fieldMap = new HashMap<>(128);
        final Field[] fields;

        public OptimizedStruct(Struct struct) {
            this.struct = struct;
            int length = struct.fields.size();
            for (Field f : struct.fields) {
                this.fieldMap.put(f.name, f);
                if (f.tag > length) {
                    length = f.tag;
                }
            }
            fields = new Field[length+1];
            for (Field f : struct.fields) {
                fields[f.tag] = f;
            }
        }
    }
}
