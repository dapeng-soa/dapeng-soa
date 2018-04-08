package com.github.dapeng.util;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;

/**
 *
 * @author lihuimin
 * @date 2017/10/27
 */
public class BeanSerializerUtil {

    public static <T> byte[] serialize(T structBean, BeanSerializer<T> structSerializer) throws TException {

        byte [] byteBuf = new byte[8192];
        final TCommonTransport outputCommonTransport = new TCommonTransport(byteBuf, TCommonTransport.Type.Write);

        TCompactProtocol outputProtocol = new TCompactProtocol(outputCommonTransport);
        structSerializer.write(structBean, outputProtocol);
        return outputCommonTransport.getByteBuf();
    }

    public static <T> T deserialize(byte[] buff, BeanSerializer<T> structSerializer) throws TException {

        final TCommonTransport inputCommonTransport = new TCommonTransport(buff, TCommonTransport.Type.Read);

        TCompactProtocol intputProtocol = new TCompactProtocol(inputCommonTransport);
        T struct = structSerializer.read(intputProtocol);
        return struct;
    }




}
