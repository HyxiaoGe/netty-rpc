package com.hyxiao.rpc.client.proxy;

import com.hyxiao.rpc.client.RpcFuture;

public interface RpcAsyncProxy {

    RpcFuture call(String funcName, Object... args);

}
