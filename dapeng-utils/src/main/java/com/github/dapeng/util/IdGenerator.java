package com.github.dapeng.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: zhup
 * @Date: 2018/4/10 9:18
 */

public class IdGenerator {

    private final static AtomicInteger ai = new AtomicInteger(0);

    /**
     * 获取trancationId,计算方式 byte[0-3] ip^ pid<<16  byte[4-7] sequenceno顺序递增 65535重置
     *
     * @param ip
     * @return
     */
    public static long genTransactionId(String ip) {
        int intIp = ipToInt(ip);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        int processId = getProcessId() << 16;
        int frontByte = intIp ^ processId;
        buffer.putInt(frontByte);
        int sequenceNo = ai.getAndIncrement();
        buffer.putInt(sequenceNo);
        if (sequenceNo == 65535) {
            ai.set(0);
        }
        buffer.flip();//need flip
        return Math.abs(buffer.getLong());
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

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i <= 65536; i++) {
            System.out.println(genTransactionId("192.168.1.3"));
        }
        System.out.println(System.currentTimeMillis() - startTime);
    }
}
