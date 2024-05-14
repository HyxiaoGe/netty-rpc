package com.hyxiao.rpc.server;

import com.hyxiao.rpc.config.provider.ProviderConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcServer {

    private String serverAddress;

    private EventLoopGroup bossGroup = new NioEventLoopGroup();

    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    private volatile Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer(String serverAddress) throws Exception {
        this.serverAddress = serverAddress;
        this.start();
    }

    private void start() throws Exception {

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new RpcServerInitializer(handlerMap));

        String[] array = serverAddress.split(":");
        String host = array[0];
        int port = Integer.parseInt(array[1]);

        ChannelFuture channelFuture = serverBootstrap.bind(host, port).sync();
        channelFuture.addListener((future) -> {
            if (future.isSuccess()) {
                log.info("server start success on host:{}. port:{}", host, port);
            } else {
                log.error("server start failed");
                throw new Exception("server start failed, cause: " + future.cause());
            }
        });

        try {
            channelFuture.await(5000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("server start failed");
            throw new Exception("server start failed, cause: " + e);
        }

    }

    public void registerProcessor(ProviderConfig providerConfig){
        handlerMap.put(providerConfig.getInterfaceClass(), providerConfig.getRef());
    }

    public void close(){
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
