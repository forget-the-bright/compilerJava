package org.hao.compiler.controller;


import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/21 10:26
 */
@RestController
@RequestMapping("/proxy")
public class ProxyController {

    @RequestMapping("/{port}/**")
    public void proxy(@PathVariable("port") int port, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String restOfThePath = extractProxyPath(request, port);
        String targetUrl = "http://localhost:" + port + restOfThePath;

        if (isSseRequest(request)) {
            handleSse(request, response, targetUrl);
        } else {
            handleHttpRequest(request, response, targetUrl);
        }
    }

    public String extractProxyPath(HttpServletRequest request, int port) {
        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String proxyPrefix = contextPath + "/proxy/" + port;
        return requestURI.substring(proxyPrefix.length());
    }


    private boolean isSseRequest(HttpServletRequest request) {
        return "text/event-stream".equalsIgnoreCase(request.getHeader("Accept"));
    }

    private void handleHttpRequest(HttpServletRequest request, HttpServletResponse response, String targetUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setRequestMethod(request.getMethod());
        connection.setDoInput(true);
        connection.setDoOutput(true);

        // 设置 Host
        connection.setRequestProperty("Host", request.getServerName());

        // 获取客户端真实 IP（考虑代理）
        String clientIp = getClientIP(request);
        connection.setRequestProperty("X-Real-IP", clientIp);

        // 设置 X-Forwarded-For
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor == null || xForwardedFor.isEmpty() || "unknown".equalsIgnoreCase(xForwardedFor)) {
            xForwardedFor = clientIp;
        } else {
            xForwardedFor += ", " + clientIp;
        }
        connection.setRequestProperty("X-Forwarded-For", xForwardedFor);

        // 设置 X-Forwarded-Proto
        connection.setRequestProperty("X-Forwarded-Proto", request.getScheme());

        // 转发原始请求头（除了 Host、Content-Length）
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (
                    !headerName.equalsIgnoreCase("Host") &&
                            !headerName.equalsIgnoreCase("Content-Length") &&
                            !headerName.equalsIgnoreCase("X-Forwarded-For") &&
                            !headerName.equalsIgnoreCase("X-Real-IP") &&
                            !headerName.equalsIgnoreCase("X-Forwarded-Proto")) {
                String headerValue = request.getHeader(headerName);
                connection.setRequestProperty(headerName, headerValue);
            }
        }

        // 转发请求体（上传文件等）
        if (request.getContentLengthLong() > 0) {
            try (InputStream in = request.getInputStream(); OutputStream out = connection.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
        }

        // 设置响应状态码
        response.setStatus(connection.getResponseCode());

        // 转发响应头
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            if (entry.getKey() == null) continue;
            //转发了目标服务的响应头（包括 Transfer-Encoding: chunked），但你的代理手动拼接了响应体，会导致 chunked 格式不正确。
            //chunked 编码格式要求响应体前要有 长度(hex)\r\n + 内容 + \r\n，否则 curl 会报错。
            if (entry.getKey().equalsIgnoreCase("Transfer-Encoding") ||
                    entry.getKey().equalsIgnoreCase("Content-Length")) {
                continue; // 不转发这些头，由代理自动处理
            }
            for (String value : entry.getValue()) {
                response.setHeader(entry.getKey(), value);
            }
        }

        // ✅ 修复点：处理响应体（包括 error stream）
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            // 如果 getInputStream() 抛出异常，尝试使用 getErrorStream()
            inputStream = connection.getErrorStream();
        }

        // 流式转发响应体
        if (inputStream != null) {
            try (InputStream in = inputStream; OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
        } else {
            // 如果都没有数据，返回空响应
            response.getWriter().write("");
        }
    }

    private void handleSse(HttpServletRequest request, HttpServletResponse response, String targetUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setRequestMethod(request.getMethod());

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        try (InputStream in = connection.getInputStream(); OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
                out.flush();
            }
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}