package com.github.dapeng.core;

import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;

/**
 * 通用编解码器接口
 * @author ever
 * @date 2017/7/17
 */
public interface BeanSerializer<T> {

    /**
     * 反序列化方法, 从Thrift协议格式中转换回PoJo
     * @param iproto
     * @return
     * @throws TException
     */
    T read(TProtocol iproto) throws TException;

    /**
     * 序列化方法, 把PoJo转换为Thrift协议格式
     * @param bean
     * @param oproto
     * @throws TException
     */
    void write(T bean, TProtocol oproto) throws TException;

    /**
     * PoJo校验方法
     * @param bean
     * @throws TException
     */
    void validate(T bean) throws TException;

    /**
     * 输出对人友好的信息
     * @param bean
     * @return
     */
    String toString(T bean);
}
