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
        sb.append("=======[" + availabe + "]\n");
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
        sb.append(" shutdown/terminating/terminated[")
                .append(poolExecutor.isShutdown()).append("/")
                .append(poolExecutor.isTerminating()).append("/")
                .append(poolExecutor.isTerminated()).append("]");
        sb.append(" -activeCount/poolSize[")
                .append(poolExecutor.getActiveCount()).append("/")
                .append(poolExecutor.getPoolSize()).append("]");
        sb.append(" -waitingTasks/completeTasks/totalTasks[")
                .append(poolExecutor.getQueue().size()).append("/")
                .append(poolExecutor.getCompletedTaskCount()).append("/")
                .append(poolExecutor.getTaskCount()).append("]");
        return sb.toString();
    }

    /**
     * transfer hex string to bytes
     * @param hex
     * @return
     */
    public static byte[] hexStr2bytes(String hex) {
        int length = hex.length()/2;
        // must be multiple of 2
        assert hex.length() % 2 == 0;
        byte[] result = new byte[length];
        for (int i = 0, j=0; j < length; j+=2, i+=4) {
            if (i + 4 > hex.length()) {
                String _2bytes = hex.substring(i, i+2) + "00";
                int anInt = Integer.parseInt(_2bytes, 16);
                result[j] = (byte)((anInt >> 8) & 0xff);
            } else {
                String _2bytes = hex.substring(i, i+4);
                int anInt = Integer.parseInt(_2bytes, 16);
                result[j] = (byte)((anInt >> 8) & 0xff);
                result[j+1] = (byte)(anInt & 0xff);
            }

        }

        return result;
    }

    public static String formatToString(String msg) {
        if (msg == null) {
            return msg;
        }

//        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (len > max_len) {
            msg = msg.substring(0, 128) + "...(" + len + ")";
        }

        return msg;
    }
}
