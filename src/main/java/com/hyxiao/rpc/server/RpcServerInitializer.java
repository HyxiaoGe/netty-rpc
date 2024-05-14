package com.hyxiao.rpc.server;

import com.hyxiao.rpc.codec.RpcDecoder;
import com.hyxiao.rpc.codec.RpcEncoder;
import com.hyxiao.rpc.codec.RpcRequest;
import com.hyxiao.rpc.codec.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.Map;

public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {

    private Map<String, Object> handlerMap;

    public RpcServerInitializer(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        socketChannel.pipeline().addLast(new RpcDecoder(RpcRequest.class));
        socketChannel.pipeline().addLast(new RpcEncoder(RpcResponse.class));
        socketChannel.pipeline().addLast(new RpcServerHandler(handlerMap));
    }
}
