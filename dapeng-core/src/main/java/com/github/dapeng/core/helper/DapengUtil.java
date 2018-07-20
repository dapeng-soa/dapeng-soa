package com.github.dapeng.core.helper;

import com.github.dapeng.core.SoaException;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dapeng.core.helper.IPUtils.localIpAsInt;

/**
 * @author ever
 * @date 20180406
 */
public class DapengUtil {
    private static int random = new Random().nextInt();
    private static AtomicInteger seqId = new AtomicInteger(random);
    private static int processId = getProcessId() << 16;
    private static int localIp = localIpAsInt();

    /**
     * 生成TransactionId. 这是一个长度为16的16进制字符串(共64bit),
     * 可用于sessionTid, callerTid, calleeTid
     * byte[0-3] ip^ pid<<32  byte[4-7] sequenceno顺序递增, Integer.MAX_VALUE重置
     *
     * @return
     */
    public static long generateTid() {
        int high = localIp ^ processId;
        int low = seqId.getAndIncrement();
        long tid = 0xffff_ffff_ffff_ffffL;
        return ((tid & high) << 32) | ((tid >>> 32) & low);
    }

    public static String longToHexStr(long tid) {
        StringBuilder sb = new StringBuilder();

        append(sb, (byte) ((tid >> 56) & 0xff));
        append(sb, (byte) ((tid >> 48) & 0xff));
        append(sb, (byte) ((tid >> 40) & 0xff));
        append(sb, (byte) ((tid >> 32) & 0xff));
        append(sb, (byte) ((tid >> 24) & 0xff));
        append(sb, (byte) ((tid >> 16) & 0xff));
        append(sb, (byte) ((tid >> 8) & 0xff));
        append(sb, (byte) ((tid) & 0xff));
        return sb.toString();
    }

    static void append(StringBuilder buffer, byte b) {
        int h = (b & 0xFF) >> 4;
        int l = b & 0x0F;

        buffer.append(h >= 10 ? (char) (h - 10 + 'a') : (char) (h + '0'));
        buffer.append(l >= 10 ? (char) (l - 10 + 'a') : (char) (l + '0'));
    }

    /**
     * 判断是否是框架异常还是业务异常
     * @param e
     * @return
     */
    public static boolean isDapengCoreException(Throwable e) {
        if (e instanceof SoaException) {
            return ((SoaException) e).getCode().startsWith("Err-Core");
        }

        return false;
    }

    /**
     * 获取当前进程id
     *
     * @return
     */
    private static int getProcessId() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return Integer.valueOf(runtimeMXBean.getName().split("@")[0])
                .intValue();
    }
}
