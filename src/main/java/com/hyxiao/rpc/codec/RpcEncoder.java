package com.hyxiao.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder<Object> {

    private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {

        if (genericClass.isInstance(msg)) {
            byte[] data = Serialization.serialize(msg);
            // 消息分为：消息头（消息的长度）+ 消息体（消息的内容）
            out.writeInt(data.length);
            out.writeBytes(data);
        }

    }
}
