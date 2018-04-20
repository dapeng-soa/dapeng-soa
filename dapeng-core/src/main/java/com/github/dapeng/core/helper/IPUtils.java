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
                NetworkInterface ni = NetworkInterface.getByName("bond0");
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
        return (SoaSystemEnvProperties.SOA_LOCAL_HOST_NAME != null && !SoaSystemEnvProperties.SOA_LOCAL_HOST_NAME.trim().isEmpty()) ? SoaSystemEnvProperties.SOA_LOCAL_HOST_NAME : inetAddress.getHostAddress();
    }


    public static String localIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    public static int localIpAsInt() {
        try {
            byte[] ip4address = InetAddress.getLocalHost().getAddress();
            return ipv4AsInt(ip4address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return 0;
        }
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

    private static int ipv4AsInt(byte[] ip4address) {
        return ((ip4address[0] & 0xff) << 24) | ((ip4address[1] & 0xff) << 16)
                | ((ip4address[2] & 0xff) << 8) | (ip4address[3] & 0xff);
    }

    public static void main(String[] args) throws UnknownHostException {
        String destination = "1.2.3/24";

        int mask = 24;

        String tagetIpSeg = "1.2.3.0";

        String callerIp = "1.2.3.128";
        String serverIp = "1.2.3.64";

        int maskIp = (0xFFFFFFFF << (32 - mask));

        System.out.println((transferIp(serverIp) & maskIp) == (transferIp(callerIp) & maskIp));

    }
}
