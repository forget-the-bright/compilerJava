package org.hao.compiler.process;

import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hao.compiler.sse.SseUtil;
import org.hao.compiler.websocket.terminal.TerminalWSUtil;
import org.hao.core.compiler.CompilerUtil;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/10 17:00
 */
@Slf4j
public class JavaRunProcess {

    private String outputDir;
    private SseEmitter emitter;
    private Process process;
    private InputStream inputStream;
    private Thread outputThread;
    private String mainClass;
    private StringBuilder command = new StringBuilder();
    private List<String> options = new ArrayList<>();

    public JavaRunProcess(String outputDir, String mainClass, SseEmitter emitter) {
        this.outputDir = outputDir;
        this.mainClass = mainClass;
        this.emitter = emitter;
    }

    private void buildCommand(String commandStr) {
        options.add(commandStr);
        command.append(commandStr.length() > 50 ? commandStr.substring(0, 50) + "..." : commandStr).append(" ");
    }

    @SneakyThrows
    public void run() {

        // 构建命令参数
        String javaHome = System.getProperty("java.home");
        String executableName = System.getProperty("os.name").toLowerCase().startsWith("windows")
                ? "java.exe"
                : "java";
        String javaPath = StrUtil.format("{}{}bin{}{}", javaHome, File.separator, File.separator, executableName);
        TreeSet<String> classpath = CompilerUtil.loadClassPath();
        classpath.add(outputDir);
        String joinClassPath = StrUtil.join(File.pathSeparator, classpath);


        // 设置 Java 命令
        buildCommand(javaPath);
        // 设置编码
        buildCommand("-Dfile.encoding=UTF-8");
        // 设置类路径
        buildCommand("-cp");
        // 构建类路径内容
        buildCommand(joinClassPath);
        // 设置主类
        buildCommand(mainClass);

        //开始执行
        ProcessBuilder processBuilder = new ProcessBuilder(options);
        process = processBuilder.start();
        inputStream = process.getInputStream();
        SseUtil.sendMegBase64Ln(emitter, TerminalWSUtil.clearCommand);
        SseUtil.sendMegBase64Ln(emitter, command.toString());
        SseUtil.sendMegBase64Ln(emitter, TerminalWSUtil.newline);
        // 读取 Shell 输出流并发送给前端
        //readOutput();
        outputThread = new Thread(this::readOutput);
        outputThread.start();
    }

    // 读取 Shell 输出并转发给前端
    private void readOutput() {
        try (InputStream in = inputStream) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                // String data = new String(buffer, 0, read, StandardCharsets.UTF_8);
                String data = new String(buffer, 0, read);
                SseUtil.sendMegBase64(emitter, data);
            }
            System.out.println("Shell output stream closed");
        } catch (IOException e) {
            log.warn("Shell output stream closed unexpectedly", e);
        } finally {
            stop();
        }
    }

    @SneakyThrows
    private void stop() {
        SseUtil.sendMegBase64Ln(emitter, TerminalWSUtil.newline);
        SseUtil.sendMegBase64Ln(emitter, "执行完毕！");
        emitter.complete();
        if (process != null) {
            process.destroy();
        }
        if (outputThread != null) {
            outputThread.interrupt();
        }
    }
}
