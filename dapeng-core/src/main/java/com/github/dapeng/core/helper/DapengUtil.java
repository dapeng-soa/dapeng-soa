package com.github.dapeng.core.helper;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dapeng.core.helper.IPUtils.ipToInt;
import static com.github.dapeng.core.helper.IPUtils.localIpAsInt;

/**
 * @author ever
 * @date 20180406
 */
public class DapengUtil {
    private static AtomicInteger seqId = new AtomicInteger(0);
    private static int processId = getProcessId() << 16;
    private static int localIp = localIpAsInt();
    /**
     * 生成TransactionId. 这是一个长度为12的16进制字符串,
     * 可用于sessionTid, callerTid, calleeTid
     * byte[0-3] ip^ pid<<16  byte[4-7] sequenceno顺序递增, Integer.MAX_VALUE重置
     *
     * @return
     */
    public static String generateTid() {
        int high = localIp ^ processId;
        int low = seqId.getAndIncrement();
        if (low == Integer.MAX_VALUE) {
            seqId.set(0);
        }

        StringBuilder sb = new StringBuilder();

        append(sb, (byte) ((high >> 24) & 0xFF));
        append(sb, (byte) ((high >> 16) & 0xFF));
        append(sb, (byte) ((high >> 8) & 0xFF));
        append(sb, (byte) ((high) & 0xFF));

        append(sb, (byte) ((low >> 24) & 0xFF));
        append(sb, (byte) ((low >> 16) & 0xFF));
        append(sb, (byte) ((low >> 8) & 0xFF));
        append(sb, (byte) ((low) & 0xFF));

        return sb.toString();
    }

    static void append(StringBuilder buffer, byte b) {
        int h = (b & 0xFF) >> 4;
        int l = b & 0x0F;

        buffer.append(h >= 10 ? (char)(h - 10 + 'a') : (char)(h + '0'));
        buffer.append(l >= 10 ? (char)(l - 10 + 'a') : (char)(l + '0'));
    }

    /**
     * 获取当前进程id
     *
     * @return
     */
    public static int getProcessId() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return Integer.valueOf(runtimeMXBean.getName().split("@")[0])
                .intValue();
    }
}
