package com.github.dapeng.client.netty;

/**
 * Created by lihuimin on 2017/12/25.
 */
public class IpPort {
    final String ip;
    final int port;

    public IpPort(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return this.ip + ": " + this.port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (obj instanceof IpPort) {
            IpPort ipPort = (IpPort) obj;
            if (ip.equals(ipPort.ip) && port == ipPort.port) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (this.port + this.ip).hashCode();
    }
}
