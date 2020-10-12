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
package com.github.dapeng.core.helper;

import java.net.*;
import java.util.Enumeration;

/**
 * IP Utils
 *
 * @author craneding
 * @date 16/1/19
 */
public class IPUtils {

    static InetAddress inetAddress = null;

    static {
        try {
            inetAddress = InetAddress.getLocalHost();

            if (inetAddress.getHostAddress() == null || "127.0.0.1".equals(inetAddress.getHostAddress())) {
                NetworkInterface bond0 = NetworkInterface.getByName("bond0");
                NetworkInterface en0 = NetworkInterface.getByName("en0");

                NetworkInterface ni = bond0!=null ? bond0: en0;

                if (ni == null)
                    throw new RuntimeException("wrong with get ip");

                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    InetAddress nextElement = ips.nextElement();
                    if ("127.0.0.1".equals(nextElement.getHostAddress()) || nextElement instanceof Inet6Address || nextElement.getHostAddress().contains(":"))
                        continue;
                    inetAddress = nextElement;
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static String containerIp() {
        return inetAddress.getHostAddress();
    }


    public static String localIp() {
        return inetAddress.getHostAddress();
    }

    public static int localIpAsInt() {
        byte[] ip4address = inetAddress.getAddress();
        return ipv4AsInt(ip4address);
    }


    public static int transferIp(String ipStr) {
        try {
            byte[] address = Inet4Address.getByName(ipStr).getAddress();

            return ipv4AsInt(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 在子网掩码下, 两个ip是否一致
     *
     * @param ip1
     * @param ip2
     * @param mask 子网掩码
     * @return
     */
    public static boolean matchIpWithMask(int ip1, int ip2, int mask) {
        int maskIp = (0xFFFFFFFF << (32 - mask));
        return (ip1 & maskIp) == (ip2 & maskIp);
    }


    /**
     * transfer ip from int to human-readable format,
     * for example:
     * -1062729086 ==> 192.168.10.130
     *
     * @param ip
     * @return
     */
    public static String transferIp(int ip) {
        return (ip >>> 24) + "."
                + (ip << 8 >>> 24) + "."
                + (ip << 16 >>> 24) + "."
                + (ip & 0x000000ff);
    }

    private static int ipv4AsInt(byte[] ip4address) {
        return ((ip4address[0] & 0xff) << 24) | ((ip4address[1] & 0xff) << 16)
                | ((ip4address[2] & 0xff) << 8) | (ip4address[3] & 0xff);
    }
}
