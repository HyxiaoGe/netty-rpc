package com.hyxiao.rpc.client;

import com.hyxiao.rpc.client.proxy.RpcAsyncProxy;
import com.hyxiao.rpc.client.proxy.RpcProxyImpl;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcClient {

    private String serverAddress;

    private long timeout;

    private final Map<Class<?>, Object> syncProxyMap = new ConcurrentHashMap<>();

    private final Map<Class<?>, Object> asyncProxyMap = new ConcurrentHashMap<>();

    public void initClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        connect();
    }

    private void connect() {
        RpcConnectManager.getInstance().connect(serverAddress);
    }

    public void stop() {
        RpcConnectManager.getInstance().stop();
    }

    @SuppressWarnings("unchecked")
    public <T> T invokeSync(Class<T> interfaceClass) {
        if (syncProxyMap.containsKey(interfaceClass)) {
            return (T) syncProxyMap.get(interfaceClass);
        }
        T proxy = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, new RpcProxyImpl<>(interfaceClass, timeout));
        syncProxyMap.put(interfaceClass, proxy);

        return proxy;
    }

    public <T> RpcAsyncProxy invokeAsync(Class<T> interfaceClass) {
        if (asyncProxyMap.containsKey(interfaceClass)) {
            return (RpcAsyncProxy) asyncProxyMap.get(interfaceClass);
        }

        return new RpcProxyImpl<>(interfaceClass, timeout);
    }


}
