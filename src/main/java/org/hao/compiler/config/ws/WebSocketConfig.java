package org.hao.compiler.config.ws;

import cn.hutool.extra.spring.SpringUtil;
import org.hao.compiler.websocket.lsp.LspWSEndPoint;
import org.hao.compiler.websocket.lsp.LspWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import javax.websocket.server.ServerEndpointConfig;

@Configuration
//启用 WebSocket 支持
@EnableWebSocket
//启用 STOMP 协议的支持，
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    /**
     * 添加原生websocket 支持
     * 注入ServerEndpointExporter，
     * 这个bean会自动注册使用了@ServerEndpoint注解声明的Websocket endpoint
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * lsp4j-websocket 原生提供
     *
     * @return 服务注册端点
     */
    @Bean
    public ServerEndpointConfig serverEndpointConfig() {
        return new ServerEndpointRegistration("/lsp4j", LspWSEndPoint.class);
    }

    /**
     * spring-boot websocket 提供
     *
     * @param registry ws处理器注册
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        LspWebSocketHandler lspWebSocketHandler = new LspWebSocketHandler();
        SpringUtil.registerBean("lspWebSocketHandler", lspWebSocketHandler);
        registry.addHandler(lspWebSocketHandler, "/lsp");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket-endpoint").withSockJS();
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(65536);
        container.setMaxBinaryMessageBufferSize(65536);
        return container;
    }

    @Bean
    public WebSocketFilter webSocketFilter() {
        return new WebSocketFilter();
    }
}

