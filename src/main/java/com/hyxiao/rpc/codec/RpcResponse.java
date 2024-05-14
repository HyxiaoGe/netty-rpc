package com.hyxiao.rpc.codec;

import lombok.Data;

import java.io.Serializable;

@Data
public class RpcResponse implements Serializable {

    private static final long serialVersionUID = -579008838510970831L;

    private String requestId;

    private Object result;

    private Throwable throwable;


}
