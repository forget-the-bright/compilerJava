package org.hao.compiler.config.web;

import org.hao.core.ip.IPUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 14:09
 */
@ControllerAdvice
public class GlobalModelAttributeAdvice {

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("title", "在线 Java 编译器");
        model.addAttribute("domainUrl", IPUtils.getBaseUrl());
        model.addAttribute("wsUrl", IPUtils.getBaseUrl().replace("http://", "ws://"));
        model.addAttribute("version", "1.0.0");
        // 可以在这里加更多通用属性
    }
}
