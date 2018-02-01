package com.github.dapeng.core.enums;

/**
 * Created by lihuimin on 2017/12/21.
 */
public enum CodecProtocol {

    Binary((byte) 0), CompressedBinary((byte) 1), Json((byte) 2), Xml((byte) 3);

    private byte code;

    private CodecProtocol(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static CodecProtocol toCodecProtocol(byte code) {
        CodecProtocol[] values = CodecProtocol.values();
        for (CodecProtocol protocol : values) {
            if (protocol.getCode() == code)
                return protocol;
        }

        return null;
    }

}
