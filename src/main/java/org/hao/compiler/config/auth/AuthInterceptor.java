package org.hao.compiler.config.auth;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.servlet.util.SaTokenContextServletUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false); // 获取session，如果不存在则返回null
        SaTokenContextServletUtil.setContext( request, response);
        StpUtil.checkLogin();
        return true; // 已登录，继续处理请求
    }
}