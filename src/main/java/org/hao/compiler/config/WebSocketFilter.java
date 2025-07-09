package org.hao.compiler.config;

import org.hao.core.ip.IPUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class WebSocketFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)servletRequest;
        // 判断是否是 WebSocket 握手请求（协议升级）
        if ("websocket".equalsIgnoreCase(httpRequest.getHeader("Upgrade"))) {
            String ipAddr = IPUtils.getIpAddr(httpRequest);
            String remoteHost = httpRequest.getRemoteHost();
            httpRequest.setAttribute("ipAddr", ipAddr);
        }

        // 继续执行后续 Filter 或目标资源（WebSocket Endpoint）
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
