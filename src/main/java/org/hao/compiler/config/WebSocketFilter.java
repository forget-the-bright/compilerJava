package org.hao.compiler.config;

import cn.hutool.core.util.ReflectUtil;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.hao.core.ip.IPUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

public class WebSocketFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        RequestFacade httpRequest = (RequestFacade) servletRequest;
        // 判断是否是 WebSocket 握手请求（协议升级）
        if ("websocket".equalsIgnoreCase(httpRequest.getHeader("Upgrade"))) {
            String ipAddr = IPUtils.getIpAddr(httpRequest);
            httpRequest.setAttribute("ipAddr", ipAddr);
            Request request = (Request) ReflectUtil.getFieldValue(httpRequest, "request");
            if (request != null) {
                HashMap<String, Object> userPrincipal = new HashMap<>();
                userPrincipal.put("ipAddr", "ipAddr");
                userPrincipal.put("username", "admin");
                request.setUserPrincipal(new ObjectPrincipal<Map<String, Object>>(userPrincipal));
            }
        }

        // 继续执行后续 Filter 或目标资源（WebSocket Endpoint）
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
