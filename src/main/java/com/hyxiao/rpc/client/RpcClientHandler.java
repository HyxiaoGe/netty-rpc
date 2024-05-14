package com.hyxiao.rpc.client;

import com.hyxiao.rpc.codec.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketAddress;

/**
 * 业务处理器
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private Channel channel;

    private SocketAddress remotePeer;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }



    public SocketAddress getRemotePeer() {
        return this.remotePeer;
    }

    //  netty 提供了一种主动关闭连接的方式，就是向服务端发送一个特殊的数据包（Unpooled.EMPTY_BUFFER），然后监听这个数据包是否发送成功，如果发送成功，就关闭连接
    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(channelFuture -> channel.close());

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {

    }
}
