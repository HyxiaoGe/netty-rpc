package com.hyxiao.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //  如果请求数据包不足4个字节直接返回，如果大于等于4个字节则读取数据包的长度
        if (in.readableBytes() < 4 ){
            return;
        }
        //  标记当前读取位置
        in.markReaderIndex();
        //  读取数据包的长度
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            //  如果数据包不完整，重置读取位置
            in.resetReaderIndex();
            return;
        }

        //  真正读取需要长度的数据包内容
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        //  反序列化，生成指定的对象
        Object obj = Serialization.deserialize(data, genericClass);

        // 填充到buff中，传递给下一个handler
        out.add(obj);

    }
}
