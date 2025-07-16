package org.hao.compiler.config.web;

import cn.dev33.satoken.exception.NotLoginException;
import org.hao.core.ip.IPUtils;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 09:31
 */
@Component
public class ErrorAttributes extends DefaultErrorAttributes {
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        super.resolveException(request, response, handler, ex);
        if (ex instanceof NotLoginException) {
            ModelAndView modelAndView = new ModelAndView("error/401");
            //添加自定义的属性
            modelAndView.addObject("reason", "完了，你写的代码又产生了一次线上事故");
            modelAndView.addObject("domainUrl", IPUtils.getBaseUrl());
            modelAndView.addObject("errorMsg", ex.getMessage());
            return modelAndView;
        } else {
            return null;
        }
    }

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        //调用父类的方法，会自动获取内置的那些属性，如果你不想要，可以不调用这个
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);

        Throwable error = getError(webRequest);
        //添加自定义的属性
        errorAttributes.put("reason", "完了，你写的代码又产生了一次线上事故");
        errorAttributes.put("domainUrl", IPUtils.getBaseUrl());
        errorAttributes.put("errorMsg", "");
        if (error != null) {
            errorAttributes.put("errorMsg", error.getMessage());
        }
        if (error instanceof NotLoginException) {
            errorAttributes.put("status", HttpStatus.UNAUTHORIZED.value());
        }
        // 你可以看一下这个方法的参数webRequest这个对象，我相信你肯定能发现好东西
        return errorAttributes;
    }

}
