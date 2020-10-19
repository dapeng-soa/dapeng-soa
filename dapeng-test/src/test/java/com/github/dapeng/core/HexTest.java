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
package com.github.dapeng.core;

import static com.github.dapeng.core.helper.DapengUtil.longToHexStr;

public class HexTest {
    public static void main(String[] args) {
        long tid = 1024783;//generateTid();
        System.out.println(tid);
        System.out.println(longToHexStr(tid));

        String hex1 = "6d7973716c7861343030";


        String hex = "0123456789ABCDEF";
        long l = 0;
        for(int i = 0; i<16; i++){
            char ch = hex.charAt(i);
            int c = (ch >= 'a') ? ch - 'a' + 10 :
                    ((ch >= 'A') ? ch - 'A' + 10 : ch - '0');
            l = l << 4 | c;
        }
        System.out.println(l);
        System.out.println( Long.parseUnsignedLong(hex, 16) );
        System.out.println(longToHexStr(l));
        System.out.println("=====");

        StringBuilder sb = new StringBuilder();
//        while(l != 0) {
//            int l4 = (int)(l & 0xF);
//            int ch = (l4 >= 10) ? l4 - 10 + 'A' : l4 + '0';
//            sb.append((char)ch);
//            l = l >>> 4;
//        }
//        System.out.println(sb.toString());

        StringBuilder sb2 = new StringBuilder();
        long b0 = l & 0x0F;
        long b1 = (l >> 4) & 0x0F;
        long b2 = (l >> 8) & 0x0F;
        long b3 = (l >> 12) & 0x0F;
        long b4 = (l >> 16) & 0x0F;
        long b5 = (l >> 20) & 0x0F;
        long b6 = (l >> 24) & 0x0F;
        long b7 = (l >> 28) & 0x0F;
        long b8 = (l >> 32) & 0x0F;
        long b9 = (l >> 36) & 0x0F;
        long b10 = (l >> 40) & 0x0F;
        long b11 = (l >> 44) & 0x0F;
        long b12 = (l >> 48) & 0x0F;
        long b13 = (l >> 52) & 0x0F;
        long b14= (l >> 56) & 0x0F;
        long b15 = (l >> 60) & 0x0F;

        sb2.append( btoc(b15) );
        sb2.append( (char)(b14 > 10 ? b14 - 10 + 'A' : b14 + '0') );
        sb2.append( (char)(b13 > 10 ? b13 - 10 + 'A' : b13 + '0') );
        sb2.append( (char)(b12 > 10 ? b12 - 10 + 'A' : b12 + '0') );
        sb2.append( (char)(b11 > 10 ? b11 - 10 + 'A' : b11 + '0') );
        sb2.append( (char)(b10 > 10 ? b10 - 10 + 'A' : b10 + '0') );
        sb2.append( (char)(b9 > 10 ? b9 - 10 + 'A' : b9 + '0') );
        sb2.append( (char)(b8 > 10 ? b8 - 10 + 'A' : b8 + '0') );
        sb2.append( (char)(b7 > 10 ? b7 - 10 + 'A' : b7 + '0') );
        sb2.append( (char)(b6 > 10 ? b6 - 10 + 'A' : b6 + '0') );
        sb2.append( (char)(b5 > 10 ? b5 - 10 + 'A' : b5 + '0') );
        sb2.append( (char)(b4 > 10 ? b4 - 10 + 'A' : b4 + '0') );
        sb2.append( (char)(b3 > 10 ? b3 - 10 + 'A' : b3 + '0') );
        sb2.append( (char)(b2 > 10 ? b2 - 10 + 'A' : b2 + '0') );
        sb2.append( (char)(b1 > 10 ? b1 - 10 + 'A' : b1 + '0') );
        sb2.append( (char)(b0 > 10 ? b0 - 10 + 'A' : b0 + '0') );

        System.out.println(sb2.toString());

        String x = Long.toHexString(l);
        System.out.println(x);

        String tmp = "6d";
        int ch = (int)Long.parseUnsignedLong(tmp, 16);
        System.out.println((byte)ch);
        char ch1 = (char)Long.parseUnsignedLong("6f", 16);
        System.out.println(ch1);
        System.out.println((int)'m');
        Byte.parseByte("6d", 16);


    }

    private static char btoc(long l) { return (char)(l > 10 ? l - 10 + 'A' : l  + '0'); }

}
