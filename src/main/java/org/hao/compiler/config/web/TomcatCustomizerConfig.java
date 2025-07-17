package org.hao.compiler.config.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 10:29
 */
//@ConditionalOnClass({Servlet.class, Tomcat.class, UpgradeProtocol.class, TomcatWebServerFactoryCustomizer.class})
@Configuration
@Slf4j
public class TomcatCustomizerConfig implements WebMvcConfigurer { //implements BeanPostProcessor
    /**
     * 方案一： 默认访问根路径跳转 doc.html页面 （swagger文档页面）
     * 方案二： 访问根路径改成跳转 index.html页面 （简化部署方案： 可以把前端打包直接放到项目的 webapp，上面的配置）
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/swagger-ui.html").setViewName("swagger-ui/index.html");
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            // 添加自定义错误页面
            factory.addContextCustomizers((context) -> {
                // Step 2: 添加自定义的 ErrorReportValve
                TomcatRootReportValve tomcatRootReportValve = new TomcatRootReportValve();
                tomcatRootReportValve.setAsyncSupported(true);
                context.getParent().getPipeline().addValve(tomcatRootReportValve);
            });
        };
    }

/*    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ConfigurableTomcatWebServerFactory) {
            ConfigurableTomcatWebServerFactory configurableTomcatWebServerFactory = (ConfigurableTomcatWebServerFactory) bean;
            addTomcat404CodePage(configurableTomcatWebServerFactory);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    private static void addTomcat404CodePage(ConfigurableTomcatWebServerFactory factory) {
        factory.addContextCustomizers((context) -> {
            // Step 2: 添加自定义的 ErrorReportValve
            CustomErrorReportValve customValve = new CustomErrorReportValve();
            customValve.setAsyncSupported(true);
            context.getParent().getPipeline().addValve(customValve);
        });
    }*/

}
