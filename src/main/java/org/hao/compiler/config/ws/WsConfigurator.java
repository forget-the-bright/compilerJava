package org.hao.compiler.config.ws;

import cn.hutool.core.util.ReflectUtil;
import org.apache.tomcat.websocket.server.WsHandshakeRequest;
import org.hao.core.ip.IPUtils;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class WsConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        WsHandshakeRequest wsHandshakeRequest = (WsHandshakeRequest) request;
        HttpServletRequest hRequest = (HttpServletRequest) ReflectUtil.getFieldValue(wsHandshakeRequest, "request");
        config.getUserProperties().put("request", hRequest);
        String ipAddr = IPUtils.getIpAddr(hRequest);
        config.getUserProperties().put("ipAddr", ipAddr);
        System.out.println();
    }
}
