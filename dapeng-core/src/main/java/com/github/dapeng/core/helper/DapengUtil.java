package com.github.dapeng.core.helper;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ever
 * @date 20180406
 */
public class DapengUtil {
    private static AtomicInteger seqId = new AtomicInteger(0);

    /**
     * 生成TransactionId. 这是一个长度为12的16进制字符串,
     * 可用于sessionTid, callerTid, calleeTid
     * byte[0-3] ip^ pid<<16  byte[4-5] sequenceno顺序递增, 65535重置
     *
     * @return
     */
    public static String generateTid() {
        int intIp = ipToInt(IPUtils.localIp());
        int processId = getProcessId() << 16;
        int heigh = intIp ^ processId;
        int low = seqId.getAndIncrement();
        if (low == Integer.MAX_VALUE) {
            seqId.set(0);
        }

        String heighBytes = Integer.toHexString(heigh);
        while (heighBytes.length() < 8) {
            heighBytes = "0" + heighBytes;
        }

        String lowBytes = Integer.toHexString(low);
        while (lowBytes.length() < 4) {
            lowBytes = "0" + lowBytes;
        }
        return heighBytes + lowBytes;
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


    /**
     * @param strIp
     * @return
     */
    public static int ipToInt(String strIp) {
        String[] ipArr = strIp.split("\\.");
        if (ipArr.length != 4) {
            return 0;
        }
        int[] ip = new int[4];
        // 先找到IP地址字符串中.的位置
        // 将每个.之间的字符串转换成整型
        ip[0] = Integer.parseInt(ipArr[0]);
        ip[1] = Integer.parseInt(ipArr[1]);
        ip[2] = Integer.parseInt(ipArr[2]);
        ip[3] = Integer.parseInt(ipArr[3]);
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }

    public  static void main(String[] args) {
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 65536; i++) {
            generateTid();
        }
        System.out.print("cost:" + (System.currentTimeMillis() - begin));
    }
}
