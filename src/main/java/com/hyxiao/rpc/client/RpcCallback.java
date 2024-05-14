package com.hyxiao.rpc.client;

public interface RpcCallback {

    void success(Object result);

    void failure(Throwable cause);

}
