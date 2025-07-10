package org.hao.compiler.process;

import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hao.compiler.sse.SseUtil;
import org.hao.core.compiler.CompilerUtil;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

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

    public JavaRunProcess(String outputDir, String mainClass, SseEmitter emitter) {
        this.outputDir = outputDir;
        this.mainClass = mainClass;
        this.emitter = emitter;
    }

    @SneakyThrows
    public void run() {
        List<String> options = new ArrayList<>();
        options.add("java");
        options.add("-Dfile.encoding=UTF-8");
        options.add("-cp");
        TreeSet<String> classpath = CompilerUtil.loadClassPath();
        classpath.add(outputDir);
        options.add(StrUtil.join(File.pathSeparator, classpath));
        options.add(mainClass);
        ProcessBuilder processBuilder = new ProcessBuilder(options);
        process = processBuilder.start();
        inputStream = process.getInputStream();
        // 读取 Shell 输出流并发送给前端
        readOutput();
//        outputThread = new Thread(this::readOutput);
//        outputThread.start();
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
        }finally {
            stop();
        }
    }

    private void stop() {
        if (process != null) {
            process.destroy();
        }
        if (outputThread != null) {
            outputThread.interrupt();
        }
    }
}
