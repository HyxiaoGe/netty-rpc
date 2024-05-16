package com.hyxiao.rpc.config.provider;

import com.hyxiao.rpc.config.RpcConfigAbstract;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProviderConfig extends RpcConfigAbstract {

    protected Object ref;

    protected String address;	// ip:port

    protected String version = "1.0.0";

    protected int weight = 1;	// 权重

}
