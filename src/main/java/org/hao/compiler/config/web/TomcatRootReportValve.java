package org.hao.compiler.config.web;

import cn.dev33.satoken.servlet.util.SaTokenContextServletUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/16 10:37
 */
@Slf4j
public class TomcatRootReportValve extends ErrorReportValve {
    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        // 如果已经提交了响应，就不处理了
        if (response.isCommitted()) {
            return;
        }
        try {
            SaTokenContextServletUtil.setContext(request, response);
            if (!StpUtil.isLogin()) {
                // 未登录，重定向到登录页
                response.sendRedirect(getContextPath(request) + "/login");
                return;
            }
            int statusCode = response.getStatus();
            if (statusCode == HttpServletResponse.SC_NOT_FOUND) {
                responseNotFoundHandler(request, response);
            } else {
                // 默认处理其他错误码
                super.report(request, response, throwable);
            }
        } catch (Exception e) {
            log.error("TomcatRootReportValve error", e);
        }finally {
            SaTokenContextServletUtil.clearContext();
        }
    }

    @SneakyThrows
    private void responseNotFoundHandler(HttpServletRequest request, HttpServletResponse response) {
        StringWriter stringWriter = new StringWriter();
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        Configuration bean = SpringUtil.getBean(Configuration.class);
        Template template = bean.getTemplate("error/404.ftl");
        // 使用 FreeMarker 渲染模板，生成主类文件内容
        Map<String, Object> data = new HashMap<>();
        data.put("domainUrl", getBaseDomainPath(request));
        template.process(data, stringWriter);
        response.getWriter().write(stringWriter.toString());
    }

    private String getContextPath(HttpServletRequest request) {
        String contextPath = ObjectUtil.defaultIfEmpty(request.getContextPath(), SpringUtil.getProperty("server.servlet.context-path"));
        contextPath = ObjectUtil.defaultIfNull(contextPath, "");
        return contextPath;
    }

    private String getBaseDomainPath(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        // 获取应用的上下文路径
        String contextPath = getContextPath(request);
        // 初始化基础URL变量
        String baseDomainPath;
        // 定义标准HTTP端口
        int httpPort = 80;
        // 根据服务器端口是否为标准HTTP端口，构造基础URL
        if (httpPort == serverPort) {
            // 如果是标准端口，URL中不显示端口号
            baseDomainPath = scheme + "://" + serverName + contextPath;
        } else {
            // 如果不是标准端口，URL中包含端口号
            baseDomainPath = scheme + "://" + serverName + ":" + serverPort + contextPath;
        }
        // 返回构造的基础URL
        return baseDomainPath;
    }
}
