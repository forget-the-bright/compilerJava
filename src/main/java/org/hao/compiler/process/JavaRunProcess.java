package org.hao.compiler.process;

import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hao.compiler.sse.SseUtil;
import org.hao.compiler.util.CompilerLocal;
import org.hao.compiler.websocket.terminal.MessageHistory;
import org.hao.compiler.websocket.terminal.TerminalWSUtil;
import org.hao.core.compiler.CompilerUtil;
import org.hao.core.print.ColorText;
import org.hao.core.print.PrintUtil;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/10 17:00
 */

@Slf4j
public class JavaRunProcess {

    private String outputDir;
    private CopyOnWriteArraySet<SseEmitter> emitters = new CopyOnWriteArraySet<>();
    private Process process;
    private InputStream inputStream;
    private Thread outputThread;
    private String mainClass;
    private MessageHistory messageHistory = new MessageHistory(1000);
    private StringBuilder command = new StringBuilder();
    private List<String> options = new ArrayList<>();

    public JavaRunProcess(String outputDir, String mainClass, SseEmitter emitter) {
        this.outputDir = outputDir;
        this.mainClass = mainClass;
        this.emitters.add(emitter);

    }

    private void buildCommand(String commandStr) {
        options.add(commandStr);
        command.append(commandStr.length() > 50 ? commandStr.substring(0, 50) + "..." : commandStr).append(" ");
//        command.append(commandStr).append(" ");
    }

    public void setEmitter(SseEmitter emitter) {
        this.emitters.add(emitter);
        messageHistory.consumerMessage(msg -> {
            try {
                SseUtil.sendMegBase64(emitter, msg);
            } catch (IOException e) {
                this.emitters.remove(emitter);
                throw new RuntimeException(e);
            }
        });
    }

    private void sendMsg(String msg) {
        for (SseEmitter sseEmitter : this.emitters) {
            try {
                SseUtil.sendMegBase64(sseEmitter, msg);
            } catch (IOException e) {
                this.emitters.remove(sseEmitter);
//                throw new RuntimeException(e);
            }
        }
    }

    private void sendMsgLn(String msg) {
        for (SseEmitter sseEmitter : this.emitters) {
            try {
                SseUtil.sendMegBase64Ln(sseEmitter, msg);
            } catch (IOException e) {
                this.emitters.remove(sseEmitter);
//                throw new RuntimeException(e);
            }
        }
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
        File workingDirectory = new File(outputDir);
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
        String workingDirectoryDir = ColorText.Builder()
                .FgBrightRed()
                .build("当前工作目录：{}", workingDirectory.getAbsolutePath());
        log.info(workingDirectoryDir);
        classpath.add(workingDirectory.getAbsolutePath());
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
        // 设置工作目录
        processBuilder.directory(workingDirectory);

        process = processBuilder.start();
        inputStream = process.getInputStream();
        sendMsgLn(TerminalWSUtil.clearCommand);
        sendMsgLn(command.toString());
        sendMsgLn(TerminalWSUtil.newline);
        // 读取 Shell 输出流并发送给前端
        //readOutput();
        outputThread = new Thread(this::readOutput);
        outputThread.setDaemon(true); // 设置为守护线程
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
                messageHistory.addMessage(data);
                sendMsg(data);
            }
            System.out.println("Shell output stream closed");
        } catch (IOException e) {
            log.warn("Shell output stream closed unexpectedly", e);
        } finally {
            stop();
        }
    }

    @SneakyThrows
    public void stop() {
        try {
            sendMsgLn(TerminalWSUtil.newline);
            sendMsgLn(PrintUtil.BLUE.getColorStr("运行结束,程序退出!!!"));
        } finally {
            this.emitters.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            CompilerLocal.clearJavaRunProcess(this);
            if (process != null) {
                process.destroyForcibly();
            }
            if (outputThread != null) {
                outputThread.interrupt();
            }
        }


    }

    // 动态销毁标识,默认false 执行默认销毁,程序自己退出,否则执行强制销毁
    private volatile boolean isExecDestory = false;

    @SneakyThrows
    public synchronized void dynamicDestory() {
        if (process != null && process.isAlive()) {
            if (!isExecDestory) {
                //这个方法会请求操作系统终止对应的子进程。但请注意，这是一个异步操作，并不保证立即完成。
                try {
                    sendMsg(PrintUtil.BLUE.getColorStr("已向进程发送退出指令"));
                } catch (Exception e) {

                }

                process.destroy(); // 请求终止进程
                isExecDestory = true;
            } else {
                // 如果进程没有响应普通的 destroy() 命令，你可以在 Java 8 及以上版本中使用 destroyForcibly();
                try {
                    sendMsg(PrintUtil.BLUE.getColorStr("已向进程发送强制退出指令"));
                } catch (Exception e) {

                }
                process.destroyForcibly(); // 强制终止进程
            }
        }
    }

    public void destroy() {
        if (process != null && process.isAlive()) {
            //这个方法会请求操作系统终止对应的子进程。但请注意，这是一个异步操作，并不保证立即完成。
            process.destroy(); // 请求终止进程
            // 如果进程没有响应普通的 destroy() 命令，你可以在 Java 8 及以上版本中使用 destroyForcibly();
            // process.destroyForcibly(); // 强制终止进程
        }
    }

    public void destroyForcibly() {
        if (process != null && process.isAlive()) {
            //这个方法会请求操作系统终止对应的子进程。但请注意，这是一个异步操作，并不保证立即完成。
            //process.destroy(); // 请求终止进程
            // 如果进程没有响应普通的 destroy() 命令，你可以在 Java 8 及以上版本中使用 destroyForcibly();
            process.destroyForcibly(); // 强制终止进程
        }
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }
}
