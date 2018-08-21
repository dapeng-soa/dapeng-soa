package com.github.dapeng.core;

public class Weight {
    public final String ip;
    public final int port;
    public final int weight;

    public Weight(String ip, int port, int weight) {
        this.ip = ip;
        this.port = port;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Weigth{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", weight=" + weight +
                '}';
    }
}
