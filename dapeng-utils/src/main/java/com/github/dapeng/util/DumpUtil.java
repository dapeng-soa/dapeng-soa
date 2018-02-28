package com.github.dapeng.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;

import java.io.ByteArrayOutputStream;

/**
 * Created by lihuimin on 2017/12/21.
 */
public class DumpUtil {

    public static void main(String[] args){
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(35);
        for(int i = 0; i<33; i++){
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        StringBuilder sb = new StringBuilder();

        // XX XX XX XX XX XX XX XX  XX XX XX XX XX XX XX XX  ASCII....
        sb.append("=======[\n");
        int i = 0;
        for (; i < availabe; i++) {
            byte b = buffer.getByte(readerIndex + i);

            String it = String.format("%02x ", b & 0xFF);
            sb.append(it);

            if (i % 16 == 15) {
                //int from = i - 15;
                sb.append(' ');
                for(int j = i - 15; j<=i; j++) {
                    char ch = (char) buffer.getByte(readerIndex + j);
                    if( ch >= 0x20 && ch < 0x7F ) sb.append(ch);
                    else sb.append('.');
                }
                sb.append("\n");
            }
        }
        i -= 1;
        int from = i / 16 * 16;
        if(i % 16 != 15) {
            for(int j = i; j % 16 != 15; j++) sb.append("   ");
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
