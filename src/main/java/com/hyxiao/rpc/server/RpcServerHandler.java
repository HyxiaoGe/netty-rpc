package com.hyxiao.rpc.server;

import com.hyxiao.rpc.codec.RpcRequest;
import com.hyxiao.rpc.codec.RpcResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS, new java.util.concurrent.ArrayBlockingQueue<>(65536));

    private Map<String, Object> handlerMap;

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
        threadPoolExecutor.submit(() -> {
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setRequestId(rpcRequest.getRequestId());
            try {
                Object result = handle(rpcRequest);
                rpcResponse.setResult(result);
            } catch (Throwable t) {
                rpcResponse.setThrowable(t);
                log.error("server handle request error", t);
            }
            ctx.writeAndFlush(rpcResponse).addListener((future) -> {
                if (future.isSuccess()) {

                }
            });
        });

    }

    private Object handle(RpcRequest request) throws InvocationTargetException {
        String className = request.getClassName();
        Object ref = handlerMap.get(className);
        Class<?> serviceClass = ref.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        FastClass serviceFastClass = FastClass.create(serviceClass);
        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);

        return serviceFastMethod.invoke(ref, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server caught exception", cause);
        ctx.close();
    }
}
