package com.github.dapeng.json;

import com.github.dapeng.core.metadata.DataType;

/**
 * @author ever
 */
public class JsonUtils {
    /**
     * 暂时只支持key为整形或者字符串的map
     *
     * @param kind
     * @return
     */
    static boolean isValidMapKeyType(DataType.KIND kind) {
        return kind == DataType.KIND.INTEGER || kind == DataType.KIND.LONG
                || kind == DataType.KIND.SHORT || kind == DataType.KIND.STRING;
    }

    /**
     * 是否集合类型
     *
     * @param kind
     * @return
     */
    static boolean isCollectionKind(DataType.KIND kind) {
        return kind == DataType.KIND.LIST || kind == DataType.KIND.SET;
    }

    /**
     * 是否容器类型
     *
     * @param kind
     * @return
     */
    static boolean isMultiElementKind(DataType.KIND kind) {
        return isCollectionKind(kind) || kind == DataType.KIND.MAP;
    }

    /**
     * 是否复杂类型
     *
     * @param kind
     * @return
     */
    static boolean isComplexKind(DataType.KIND kind) {
        return isMultiElementKind(kind) || kind == DataType.KIND.STRUCT;
    }
}
