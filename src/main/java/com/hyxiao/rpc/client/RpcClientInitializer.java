package com.hyxiao.rpc.client;

import com.hyxiao.rpc.codec.RpcDecoder;
import com.hyxiao.rpc.codec.RpcEncoder;
import com.hyxiao.rpc.codec.RpcRequest;
import com.hyxiao.rpc.codec.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        //  添加编码器和解码器
        //  client 端：对请求进行编码，对响应进行解码
        socketChannel.pipeline().addLast(new RpcEncoder(RpcRequest.class));
        socketChannel.pipeline().addLast(new RpcDecoder(RpcResponse.class));
        // lengthFieldOffset = 0 ，长度字段的偏移量，表示跳过的字节数
        // lengthFieldLength = 4 ，长度字段的长度，表示长度字段占几个字节
        // 即0-4位是数据包头，数据包头的长度
        socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        socketChannel.pipeline().addLast(new RpcClientHandler());
    }
}
