package org.hao.compiler.config.web;

import org.hao.core.ip.IPUtils;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 09:31
 */
@Component
public class ErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        //调用父类的方法，会自动获取内置的那些属性，如果你不想要，可以不调用这个
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);

        //添加自定义的属性
        errorAttributes.put("reason", "完了，你写的代码又产生了一次线上事故");
        errorAttributes.put("domainUrl", IPUtils.getBaseUrl());
        // 你可以看一下这个方法的参数webRequest这个对象，我相信你肯定能发现好东西

        return errorAttributes;
    }

}
