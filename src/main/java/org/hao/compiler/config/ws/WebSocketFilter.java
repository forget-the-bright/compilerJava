package org.hao.compiler.config.ws;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ReflectUtil;
import org.apache.catalina.connector.Request;
import org.hao.core.ip.IPUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebSocketFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        // 判断是否是 WebSocket 握手请求（协议升级）
        if ("websocket".equalsIgnoreCase(httpRequest.getHeader("Upgrade"))) {
            String ipAddr = IPUtils.getIpAddr(httpRequest);
            httpRequest.setAttribute("ipAddr", ipAddr);
            Object objRequest = ReflectUtil.getFieldValue(httpRequest, "request");
            while (objRequest != null) {
                if (objRequest instanceof Request) {
                    break;
                }
                objRequest = ReflectUtil.getFieldValue(objRequest, "request");
            }
            if (objRequest != null) {
                Request request = (Request) objRequest;
                HashMap<String, Object> userPrincipal = new HashMap<>();
                request.setUserPrincipal(new ObjectPrincipal<Map<String, Object>>(userPrincipal));
                userPrincipal.put("ipAddr", ipAddr);
                userPrincipal.put("username", null);
                userPrincipal.put("user", null);
                try {
                    StpUtil.checkLogin();
                    userPrincipal.put("username", StpUtil.getLoginId());
                    userPrincipal.put("user", StpUtil.getSession(true).get("user"));
                } catch (NotLoginException nlp) {
                    userPrincipal.put("errorMsg", nlp.getMessage());
                    // return;
                }

            }
        }
        // 继续执行后续 Filter 或目标资源（WebSocket Endpoint）
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
