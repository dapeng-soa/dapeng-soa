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

    public static String localIp() {
        return (SoaSystemEnvProperties.SOA_LOCAL_HOST_NAME != null && !SoaSystemEnvProperties.SOA_LOCAL_HOST_NAME.trim().isEmpty()) ? SoaSystemEnvProperties.SOA_LOCAL_HOST_NAME : inetAddress.getHostAddress();
    }


    public static String getCallerIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}
