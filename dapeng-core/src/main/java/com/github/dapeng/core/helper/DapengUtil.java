package com.github.dapeng.core.helper;

import java.util.UUID;

/**
 * @author ever
 * @date 20180406
 */
public class DapengUtil {

    /**
     * 生成TransactionId.
     * 可用于sessionTid, callerTid, calleeTid
     * @return
     */
    public static String generateTid() {
        return UUID.randomUUID().toString();
    }
}
