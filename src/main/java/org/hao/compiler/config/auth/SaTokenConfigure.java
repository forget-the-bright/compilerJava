package org.hao.compiler.config.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
       // registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
    // 注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
        registry
                .addInterceptor(new AuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/proxy/**",
                        "/error",
                        "/**/error",
                        "/error/**",
                        "/**/error/**",
                        "/login",
                        "/register",
                        "/favicon.ico",      // 放过网站图标
                        "/static/**",       // 放过 static 目录下的所有资源
                        "/assets/**",       // 放过 assets 目录下的所有资源
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/**/*.js",
                        "/**/*.css",
                        "/**/*.html",
                        "/**/*.svg",
                        "/**/*.pdf",
                        "/**/*.jpg",
                        "/**/*.png",
                        "/**/*.gif",
                        "/**/*.ico",
                        "/**/*.ttf",
                        "/**/*.woff",
                        "/**/*.woff2",
                        "/druid/**",
                        "/doc.html",

                        "/swagger-ui.html",
                        "/**/swagger-ui.html",

                        "/swagger-ui/**",
                        "/**/swagger-ui/**",

                        "/swagger-ui/*.html",
                        "/**/swagger-ui/*.html",
                        "/webjars/**",
                        "/v1/**",
                        "/v2/**",
                        "/v3/**",
                        "/**/*.js.map",
                        "/**/*.css.map"
                );
        //        registry
//                .addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
//                .addPathPatterns("/**")
//                .excludePathPatterns("/login");
        //update-begin--Author:scott Date:20221116 for：排除静态资源后缀
    }
}

