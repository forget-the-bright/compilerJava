package org.hao.compiler.config;

import cn.dev33.satoken.servlet.util.SaTokenContextServletUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class SaContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            SaTokenContextServletUtil.setContext((HttpServletRequest) request, (HttpServletResponse) response);
            filterChain.doFilter(request, response);
        } finally {
            SaTokenContextServletUtil.clearContext();
        }
    }

}
