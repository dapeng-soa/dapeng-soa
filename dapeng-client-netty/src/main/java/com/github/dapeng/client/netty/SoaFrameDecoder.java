package com.github.dapeng.client.netty;

import com.github.dapeng.util.DumpUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.github.dapeng.core.SoaProtocolConstants.ETX;
import static com.github.dapeng.core.SoaProtocolConstants.STX;

/**
 * A decoder that splits the received {@link ByteBuf}s by the number of bytes which takes the first 4 bytes.
 * For example, if you received the following four fragmented packets:
 * <pre>
 * +---+----+------+----+----+--+
 * | 000AAB | C | DEF0007G | HI |
 * +---+----+------+----+----+--+
 * </pre>
 * A {@link SoaFrameDecoder}{@code ()} will decode them into the
 * following two packets:
 * <pre>
 * +-----+-----+-----+----+
 * | 000AABCDEF | 0007GHI |
 * +-----+-----+-----+----+
 * </pre>
 *
 * actually, here's the protocol for dapeng:
 * length(4) stx(1) version(1) protocol(1) seqid(4) header(...) body(...) etx(1)
 *
 * @author craneding
 * @date 16/1/12
 */
public class SoaFrameDecoder extends ByteToMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaFrameDecoder.class);

    public SoaFrameDecoder() {
        setSingleDecode(false);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // waiting for four bytes
        if (in.readableBytes() < Integer.BYTES) return;

        int readerIndex = in.readerIndex();

        int length = in.readInt();

        if (length == 0)// 心跳
            return;

        // waiting for complete
        if (in.readableBytes() < length) {
            in.readerIndex(readerIndex);

            return;
        }

        // length(4) stx(1) version(1) protocol(1) seqid(4) header(...) body(...) etx(1)
        byte stx = in.readByte();
        if (stx != STX) {
            ctx.close();
            LOGGER.error(getClass().getSimpleName() + "::decode:通讯包起始符异常, 连接关闭");
            return;
        }

        in.skipBytes(length -2);
        byte etx = in.readByte();
        if (etx != ETX) {
            ctx.close();
            LOGGER.error(getClass().getSimpleName() + "::decode:通讯包结束符异常, 连接关闭");
            return;
        }

        in.readerIndex(readerIndex);

        ByteBuf msg = in.slice(readerIndex, length + Integer.BYTES).retain();

        /**
         * 将readerIndex放到报文尾，否则长连接收到第二次报文时会出现不可预料的错误
         */
        in.readerIndex(readerIndex + length + Integer.BYTES);

        out.add(msg);
    }

}
