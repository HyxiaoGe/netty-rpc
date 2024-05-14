package com.hyxiao.rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RpcConnectManager {

    private static volatile RpcConnectManager RPC_CONNECT_MANAGER = new RpcConnectManager();

    private RpcConnectManager() {
    }

    public static RpcConnectManager getInstance() {
        return RPC_CONNECT_MANAGER;
    }

    /**
     * 一个连接的地址，对应一个实际的业务处理器（client）
     */
    private Map<InetSocketAddress, RpcClientHandler> connectedHandlerMap = new ConcurrentHashMap<>();

    //  用于存储连接成功的handler
    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers = new CopyOnWriteArrayList<>();

    //  用于异步的提交连接请求的线程池
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private ReentrantLock connectedLock = new ReentrantLock();

    private Condition connectedCondition = connectedLock.newCondition();

    private volatile boolean isRunning = true;

    private AtomicInteger handlerIdx = new AtomicInteger(0);

    public void connect(final String serverAddress) {
        List<String> allServerAddress = Arrays.asList(serverAddress.split(","));
        updateConnectedServer(allServerAddress);
    }

    /**
     * 更新缓存信息，并异步发起连接
     * 对于连接进来的资源做一个缓存（做一个管理）
     *
     */
    private void updateConnectedServer(List<String> allServerAddress) {

        if (allServerAddress.isEmpty()) {
            log.error("no available server address!!!");
            return;
        }
        //  1. 解析allServerAddress地址，并且临时存储到newAllServerNodeSet中
        HashSet<InetSocketAddress> newAllServerNodeSet = new HashSet<>();
        for (String address : allServerAddress) {
            log.info("connect to server address: {}", address);
            String[] addrArray = address.split(":");
            if (addrArray.length == 2) {
                String host = addrArray[0];
                int port = Integer.parseInt(addrArray[1]);
                final InetSocketAddress remotePeer = new InetSocketAddress(host, port);
                newAllServerNodeSet.add(remotePeer);
            }
        }
        //  2. 调用建立连接方法，发起远程连接操作
        for (InetSocketAddress socketAddress : newAllServerNodeSet) {
            //  遍历newAllServerNodeSet，对于每个地址，检查connectedHandlerMap是否已经有对应的连接处理器：如果没有，那么发起连接
            if (!connectedHandlerMap.containsKey(socketAddress)) {
                //  3. 异步发起连接
                connectAsync(socketAddress);
            }
        }
        //  4.如果allServerAddress列表里不存在的连接地址，那么需要从缓存中移除
        for (int i = 0; i < connectedHandlers.size(); i++) {
            RpcClientHandler rpcClientHandler = connectedHandlers.get(i);
            SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
            if (!newAllServerNodeSet.contains(remotePeer)) {
                log.info("remove invalid server node {}", remotePeer);
                RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
                if (handler != null) {
                    handler.close();
                    connectedHandlerMap.remove(remotePeer);
                }
                connectedHandlers.remove(rpcClientHandler);
            }
        }

    }

    /***
     * 异步连接
     * @param socketAddress
     */
    private void connectAsync(InetSocketAddress socketAddress) {
        threadPoolExecutor.submit(() -> {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap
                    .group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new RpcClientInitializer());
            //  调用connect方法发起实际的网络连接。
            connect(bootstrap, socketAddress);
        });
    }

    private void connect(Bootstrap bootstrap, InetSocketAddress socketAddress) {
        //  建立连接
        ChannelFuture channelFuture = bootstrap.connect(socketAddress);
        //  连接失败监听
        channelFuture.channel().closeFuture().addListener((future) -> {
            log.info("channel closed, address: {}", socketAddress);
            channelFuture.channel().eventLoop().schedule(() -> {
                log.info("reconnect to server: {}", socketAddress);
                clearConnected();
                connect(bootstrap, socketAddress);
            }, 3, TimeUnit.SECONDS);
        });
        //  连接成功监听
        channelFuture.addListener((future) -> {
            if (future.isSuccess()) {
                log.info("connect to server: {} success", socketAddress);
                RpcClientHandler rpcClientHandler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                addHandler(rpcClientHandler);
            }
        });

    }

    private void addHandler(RpcClientHandler rpcClientHandler) {
        connectedHandlers.add(rpcClientHandler);
        InetSocketAddress remoteAddress = (InetSocketAddress) rpcClientHandler.getRemotePeer();
        connectedHandlerMap.put(remoteAddress, rpcClientHandler);

        signalAvailableHandler();
    }

    /**
     * 唤醒另外一端的线程（堵塞的状态中）告知有新连接接入
     */
    private void signalAvailableHandler() {
        connectedLock.lock();
        try {
            connectedCondition.signalAll();
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 等待连接可用
     * @return 是否连接可用
     */
    private boolean waitAvailableHandler() throws InterruptedException {
        connectedLock.lock();

        try {
            long connectTimeoutMills = 6000;
            return connectedCondition.await(connectTimeoutMills, TimeUnit.MILLISECONDS);
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 选择一个RpcClientHandler
     * @return
     */
    public RpcClientHandler chooseHandler() {
        CopyOnWriteArrayList<RpcClientHandler> handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlers.clone();
        int size = handlers.size();
        while (isRunning && size <= 0) {
            try {
                boolean available = waitAvailableHandler();
                if (available) {
                    handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlers.clone();
                    size = handlers.size();
                }
            } catch (InterruptedException e) {
                log.error("waitAvailableHandler error", e);
                throw new RuntimeException("no connection");
            }
        }
        if (!isRunning) {
            return null;
        }
        int index = handlerIdx.getAndAdd((1 + size) % size);

        return handlers.get(index);
    }

    public void stop() {
        isRunning = false;
        for (RpcClientHandler rpcClientHandler : connectedHandlers) {
            rpcClientHandler.close();
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    /**
     * 负责清理所有活跃的连接
     * 主要目的是在需要断开所有连接时，如系统关闭或重新连接前的清理工作，确保资源得到适当释放并维护连接列表的准确性。
     */
    private void clearConnected() {
        for (RpcClientHandler rpcClientHandler : connectedHandlers) {
            //  通过RpcClientHandler 找到具体的remotePeer，从connectedHandlerMap进行移除指定的RpcClientHandler
            SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
            RpcClientHandler clientHandler = connectedHandlerMap.get(remotePeer);
            if (clientHandler != null) {
                clientHandler.close();
                connectedHandlerMap.remove(remotePeer);
            }
        }

        connectedHandlers.clear();
    }

    /**
     * 重新连接
     * @param handler
     * @param remotePeer
     */
    public void reconnect(final RpcClientHandler handler, final SocketAddress remotePeer) {
        if (handler != null) {
            handler.close();
            connectedHandlers.remove(handler);
            connectedHandlerMap.remove(remotePeer);
        }
        connectAsync((InetSocketAddress) remotePeer);
    }

}
