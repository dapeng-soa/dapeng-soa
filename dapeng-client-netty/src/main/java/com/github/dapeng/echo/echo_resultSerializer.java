package com.github.dapeng.echo;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.SoaCode;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;

public class echo_resultSerializer implements BeanSerializer<echo_result> {
    @Override
    public echo_result read(TProtocol iprot) throws TException {

        echo_result bean = new echo_result();
        com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();

        while (true) {
            schemeField = iprot.readFieldBegin();
            if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                break;
            }

            switch (schemeField.id) {
                case 0:  //SUCCESS
                    if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                        bean.setSuccess(iprot.readString());
                    } else {
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                    }
                    break;
                default:
                    com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        validate(bean);
        return bean;
    }

    @Override
    public void write(echo_result bean, TProtocol oprot) throws TException {

        validate(bean);
        oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("echo_result"));

        oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 0));
        oprot.writeString(bean.getSuccess());
        oprot.writeFieldEnd();

        oprot.writeFieldStop();
        oprot.writeStructEnd();
    }

    @Override
    public void validate(echo_result bean) throws TException {

        if (bean.getSuccess() == null)
            throw new SoaException(SoaCode.RespFieldNull, "success字段不允许为空");
    }

    @Override
    public String toString(echo_result bean) {
        return bean == null ? "null" : bean.toString();
    }
}
