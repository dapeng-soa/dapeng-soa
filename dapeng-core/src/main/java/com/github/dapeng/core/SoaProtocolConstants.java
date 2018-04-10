package com.github.dapeng.core;

/**
 * Dapeng soa protocol constants
 * length(4) stx(1) version(1) protocol(1) seqid(4) header(...) body(...) etx(1)
 * @author ever
 * @date 2018/03/31
 */
public class SoaProtocolConstants {
    /**
     * Frame begin flag
     */
    public static final byte STX = 0x02;
    /**
     * Frame end flag
     */
    public static final byte ETX = 0x03;
    /**
     * Soa version
     */
    public static final byte VERSION = 1;
}
