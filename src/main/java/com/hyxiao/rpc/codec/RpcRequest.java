package com.hyxiao.rpc.codec;

import java.io.Serializable;

public class RpcRequest implements Serializable {

    private static final long serialVersionUID = 9000920774943105957L;

    private String requestId;

    private String className;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] parameters;


}
