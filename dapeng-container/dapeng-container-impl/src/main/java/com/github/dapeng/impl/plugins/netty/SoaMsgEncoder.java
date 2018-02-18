package com.github.dapeng.impl.plugins.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class SoaMsgEncoder<RESP> extends MessageToByteEncoder<RESP> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RESP msg, ByteBuf out) throws Exception {
        if (!(msg instanceof ByteBuf)) {

        }
    }
}
