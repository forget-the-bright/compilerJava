package org.hao.compiler.config.auth;

import cn.dev33.satoken.servlet.util.SaTokenContextServletUtil;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false); // 获取session，如果不存在则返回null
        try {
            SaTokenContextServletUtil.setContext((HttpServletRequest) request, (HttpServletResponse) response);
            if (!StpUtil.isLogin()) {
                // 未登录，重定向到登录页
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        } finally {
          //  SaTokenContextServletUtil.clearContext();
        }
        return true; // 已登录，继续处理请求
    }
}