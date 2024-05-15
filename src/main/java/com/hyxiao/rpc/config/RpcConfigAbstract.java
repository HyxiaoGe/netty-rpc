package com.hyxiao.rpc.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class RpcConfigAbstract {

    private AtomicInteger generator = new AtomicInteger();

    protected String id;

    @Setter
    @Getter
    protected String interfaceClass = null;

    //  服务的调用方
    protected Class<?> proxyClass = null;

    public String getId() {
        if (StringUtils.isBlank(id)) {
            id = "config-gen-" + generator.getAndIncrement();
        }
        return id;
    }

    public void setId(){
        this.id = id;
    }

}
