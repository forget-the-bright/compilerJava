package org.hao.compiler.config.web;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.servlet.Servlet;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 10:29
 */
//@ConditionalOnClass({Servlet.class, Tomcat.class, UpgradeProtocol.class, TomcatWebServerFactoryCustomizer.class})
@Configuration
@Slf4j
public class TomcatCustomizerConfig { //implements BeanPostProcessor
    @Bean
    public WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            // 添加自定义错误页面
            factory.addContextCustomizers((context) -> {

   /*         Arrays.stream(context.getPipeline().getValves())
                    .filter(valve -> valve instanceof ErrorReportValve)
                    .findFirst()
                    .ifPresent(v -> context.getPipeline().removeValve(v));*/

                // Step 2: 添加自定义的 ErrorReportValve
                CustomErrorReportValve customValve = new CustomErrorReportValve();
                //customValve.setProperty("errorCode.404", "/complier/error");
                //customValve.setShowServerInfo(false);
                //customValve.setShowReport(false);
                customValve.setAsyncSupported(true);
                context.getParent().getPipeline().addValve(customValve);
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
