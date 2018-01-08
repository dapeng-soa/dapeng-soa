package com.github.dapeng.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;

import java.io.ByteArrayOutputStream;

/**
 * Created by lihuimin on 2017/12/21.
 */
public class DumpUtil {

    public static void main(String args[]){
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(35);
        for(int i = 0; i<33; i++){
            buffer.writeByte('A' + i);
        }
        buffer.readerIndex(0);

        dump(buffer);
    }

    public static void dump(ByteBuf buffer) {
        int readerIndex = buffer.readerIndex();
        int availabe = buffer.readableBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // XX XX XX XX XX XX XX XX  XX XX XX XX XX XX XX XX  ASCII....
        System.out.println("=======[");
        int i = 0;
        for (; i < availabe; i++) {
            byte b = buffer.getByte(readerIndex + i);

            String it = String.format("%02x ", b & 0xFF);
            System.out.print(it);

            if (i % 16 == 15) {
                //int from = i - 15;
                System.out.print(' ');
                for(int j = i - 15; j<=i; j++) {
                    char ch = (char) buffer.getByte(readerIndex + j);
                    if( ch >= 0x20 && ch < 0x7F ) System.out.print(ch);
                    else System.out.print('.');
                }
                System.out.println();
            }
        }
        i -= 1;
        int from = i / 16 * 16;
        if(i % 16 != 15) {
            for(int j = i; j % 16 != 15; j++) System.out.print("   ");
            System.out.print(' ');
            for (int j = from; j <= i; j++) {
                char ch = (char) buffer.getByte(readerIndex + j);
                if (ch >= 0x20 && ch < 0x7F) System.out.print(ch);
                else System.out.print('.');
            }
            System.out.println();
        }

    }
}
