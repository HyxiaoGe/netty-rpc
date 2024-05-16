package com.hyxiao.rpc.config.provider;

import com.hyxiao.rpc.server.RpcServer;
import lombok.Data;

import java.util.List;

@Data
public class RpcServerConfig {

    private final String host = "127.0.0.1";

    protected int port;

    private List<ProviderConfig> providerConfigs;

    private RpcServer rpcServer = null;

    public RpcServerConfig(List<ProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }

    public void exporter() {
        if (rpcServer == null) {
            try {
                rpcServer = new RpcServer(host + ":" + port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (ProviderConfig providerConfig : providerConfigs) {
                rpcServer.registerProcessor(providerConfig);
            }
        }
    }

}
