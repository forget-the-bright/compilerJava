package org.hao.compiler.config.web;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import org.hao.core.StrUtil;
import org.hao.core.ip.IPUtils;
import org.hao.core.print.ColorText;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

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
public class GlobalErrorAttributes extends DefaultErrorAttributes {
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        super.resolveException(request, response, handler, ex);
        if (handler instanceof ResourceHttpRequestHandler) {
            ColorText.Builder()
                    .BgBrightCyan()
                    .FgBlue()
                    .FontBold()
                    .Println("系统异常[{}],访问ip [{}] , 接口 {} : 但是不存在对应的处理器,跳转404",
                            ex.getClass().getSimpleName(),
                            IPUtils.getIpAddr(request),
                            request.getRequestURI());
            return NotFoundExceptionHandler(request, response);
        }
        if (ex instanceof NotLoginException) {
            ColorText.Builder()
                    .BgBrightYellow()
                    .FgRed()
                    .FontBold()
                    .Println("鉴权失败,访问ip [{}] , 接口 {} : 存在对应的处理器,进入错误分类处理逻辑",
                            IPUtils.getIpAddr(request), request.getRequestURI());
            return NotLoginExceptionHandler((NotLoginException) ex, response);
        }
        return null;
    }

    private ModelAndView NotFoundExceptionHandler(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView("error/404");
        modelAndView.addObject("errorMsg", StrUtil.formatFast("{} : 不存在对应的处理器", request.getRequestURI()));
        modelAndView.addObject("domainUrl", IPUtils.getBaseUrl());
        return modelAndView;
    }

    /**
     * 登录异常处理器
     *
     * @param nle
     * @param response
     * @return 处理错误的视图对象
     */
    private ModelAndView NotLoginExceptionHandler(NotLoginException nle, HttpServletResponse response) {
        ModelAndView modelAndView;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        modelAndView = new ModelAndView("error/401");
        //添加自定义的属性
        modelAndView.addObject("domainUrl", IPUtils.getBaseUrl());
        modelAndView.addObject("errorMsg", nle.getMessage());
        return modelAndView;
    }

    /**
     * 处理其他异常时参数的设置
     *
     * @param webRequest the source request
     * @param options    options for error attribute contents
     * @return
     */
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
