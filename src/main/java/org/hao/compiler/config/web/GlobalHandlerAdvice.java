package org.hao.compiler.config.web;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.servlet.util.SaTokenContextServletUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import org.hao.compiler.entity.User;
import org.hao.core.ip.IPUtils;
import org.hao.core.thread.ThreadUtil;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletResponse;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 14:09
 */
@ControllerAdvice
public class GlobalHandlerAdvice {

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("title", "在线 Java 编译器");
        model.addAttribute("domainUrl", IPUtils.getBaseUrl());
        model.addAttribute("wsUrl", IPUtils.getBaseUrl().replace("http://", "ws://"));
        model.addAttribute("version", "1.0.0");

        SaTokenContextServletUtil.setContext(ThreadUtil.getRequest(), ThreadUtil.getResponse());
        if (StpUtil.isLogin()) {
            model.addAttribute("username", StpUtil.getLoginId());
            User user = (User) StpUtil.getSession(true).get("user");
            model.addAttribute("nickname", user.getNickName());
        }
        // 可以在这里加更多通用属性
    }

    // 全局异常拦截（拦截项目中的NotLoginException异常）
    //@ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED) //RedirectView
    public RedirectView handlerNotLoginException(NotLoginException nle, RedirectAttributes redirectAttributes) {
        String message = getNotLoginExceptionMessage(nle);
        // String format = StrUtil.format("redirect:/login{}", StrUtil.isEmpty(message) ? message : "?error=" + message);
        if (StrUtil.isNotEmpty(message)) {
            // 添加需要传递的参数
            redirectAttributes.addFlashAttribute("error", message);
            redirectAttributes.addFlashAttribute("errorMsg", message);
        }
        // 返回给前端
        RedirectView redirectView = new RedirectView("/login", true);
        redirectView.setStatusCode(HttpStatus.UNAUTHORIZED);
        return redirectView; // 第二个参数为true表示保留查询参数
    }

    @Deprecated
    @ExceptionHandler(NotLoginException.class)
    public ModelAndView handlerNotLoginException(NotLoginException nle, HttpServletResponse response) {
        // String message = getNotLoginExceptionMessage(nle);
        ModelAndView modelAndView;
        if (nle.getType().equals(NotLoginException.NOT_TOKEN)) {
            StpUtil.logout();
            modelAndView = new ModelAndView("redirect:/login");
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            modelAndView = new ModelAndView("error/401");
        }
        modelAndView.addObject("errorMsg", nle.getMessage());
        modelAndView.addObject("title", "在线 Java 编译器");
        modelAndView.addObject("domainUrl", IPUtils.getBaseUrl());
        modelAndView.addObject("wsUrl", IPUtils.getBaseUrl().replace("http://", "ws://"));
        modelAndView.addObject("version", "1.0.0");
        return modelAndView;
    }

    private String getNotLoginExceptionMessage(NotLoginException nle) {
        // 判断场景值，定制化异常信息
        String message = "";
        if (nle.getType().equals(NotLoginException.NOT_TOKEN)) {
            message = "未能读取到有效 token";
        } else if (nle.getType().equals(NotLoginException.INVALID_TOKEN)) {
            message = "token 无效";
        } else if (nle.getType().equals(NotLoginException.TOKEN_TIMEOUT)) {
            message = "token 已过期";
        } else if (nle.getType().equals(NotLoginException.BE_REPLACED)) {
            message = "token 已被顶下线";
        } else if (nle.getType().equals(NotLoginException.KICK_OUT)) {
            message = "token 已被踢下线";
        } else if (nle.getType().equals(NotLoginException.TOKEN_FREEZE)) {
            message = "token 已被冻结";
        } else if (nle.getType().equals(NotLoginException.NO_PREFIX)) {
            message = "未按照指定前缀提交 token";
        } else {
            message = "当前会话未登录";
        }
        //StpUtil.logout();
        return message;
    }
}
