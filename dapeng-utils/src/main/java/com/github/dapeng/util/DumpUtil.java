package com.github.dapeng.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author lihuimin
 * @date 2017/12/21
 */
public class DumpUtil {

    public static void main(String[] args) {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(35);
        for (int i = 0; i < 33; i++) {
            buffer.writeByte('A' + i);
        }
        buffer.readerIndex(0);

        dump(buffer);
    }

    public static void dump(ByteBuf buffer) {
        System.out.println(dumpToStr(buffer));
    }

    public static String dumpToStr(ByteBuf buffer) {
        int readerIndex = buffer.readerIndex();
        int availabe = buffer.readableBytes();

        StringBuilder sb = new StringBuilder();

        // XX XX XX XX XX XX XX XX  XX XX XX XX XX XX XX XX  ASCII....
        sb.append("=======[" + availabe + "\n");
        int i = 0;
        for (; i < availabe; i++) {
            byte b = buffer.getByte(readerIndex + i);

            String it = String.format("%02x ", b & 0xFF);
            sb.append(it);

            if (i % 16 == 15) {
                //int from = i - 15;
                sb.append(' ');
                for (int j = i - 15; j <= i; j++) {
                    char ch = (char) buffer.getByte(readerIndex + j);
                    if (ch >= 0x20 && ch < 0x7F) sb.append(ch);
                    else sb.append('.');
                }
                sb.append("\n");
            }
        }
        i -= 1;
        int from = i / 16 * 16;
        if (i % 16 != 15) {
            for (int j = i; j % 16 != 15; j++) sb.append("   ");
            sb.append(' ');
            for (int j = from; j <= i; j++) {
                char ch = (char) buffer.getByte(readerIndex + j);
                if (ch >= 0x20 && ch < 0x7F) sb.append(ch);
                else sb.append('.');
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 返回线程池的状况
     * 包括:线程池状态/活动线程数/当前总线程数/当前完成任务数/总任务数
     *
     * @param poolExecutor
     * @return
     */
    public static String dumpThreadPool(ThreadPoolExecutor poolExecutor) {
        StringBuilder sb = new StringBuilder();
        sb.append(" shutdown / terminating / terminated[")
                .append(poolExecutor.isShutdown()).append(" / ")
                .append(poolExecutor.isTerminating()).append(" / ")
                .append(poolExecutor.isTerminated()).append("]");
        sb.append(" -activeCount / poolSize[")
                .append(poolExecutor.getActiveCount()).append(" / ")
                .append(poolExecutor.getPoolSize()).append("]");
        sb.append(" -waitingTasks / completeTasks / totalTasks[")
                .append(poolExecutor.getQueue().size()).append(" / ")
                .append(poolExecutor.getCompletedTaskCount()).append(" / ")
                .append(poolExecutor.getTaskCount()).append("]");
        return sb.toString();
    }

    public static String formatToString(String msg) {
        if (msg == null) {
            return msg;
        }

        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (len > max_len) {
            msg = msg.substring(0, 128) + "...(" + len + ")";
        }

        return msg;
    }
}
