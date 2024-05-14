package com.hyxiao.rpc.config.provider;

import com.hyxiao.rpc.config.RpcConfigAbstract;

public class ProviderConfig extends RpcConfigAbstract {

    protected Object ref;

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
