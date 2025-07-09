package org.hao.compiler.websocket;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hao.vo.Tuple;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/9 11:56
 */
@Component
@ServerEndpoint("/terminalWS/{SessionId}") //此注解相当于设置访问URL
@Slf4j
public class TerminalWSHandler {
    private Session session;
    private PtyProcess shellProcess;
    private OutputStream shellInput;
    // 用于读取 Shell 输出并发送给前端
    private Thread outputThread;

    @OnOpen
    @SneakyThrows
    public void onOpen(Session session,
                       @PathParam(value = "SessionId") String SessionId) {
        this.session = session;

        //ProcessBuilder pb;
        Tuple<String[], Map> shellCommand = getShellCommand();
        String[] cmd = shellCommand.getFirst();
        Map<String, String> env = shellCommand.getSecond();
        shellProcess = new PtyProcessBuilder().setCommand(cmd).setEnvironment(env).start();

        //pb.redirectErrorStream(true); // 合并 stdout 和 stderr
        // shellProcess = pb.start();

        //shellInput = shellProcess.getOutputStream();
        shellInput = shellProcess.getOutputStream();

        // 读取 Shell 输出流并发送给前端
        outputThread = new Thread(this::readOutput);
        outputThread.start();

    }

    // 读取 Shell 输出并转发给前端
    private void readOutput() {
        try (InputStream in = shellProcess.getInputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                String data = new String(buffer, 0, read, StandardCharsets.UTF_8);
                sendToClient(data);
            }
            System.out.println("Shell output stream closed");
        } catch (IOException e) {
            log.warn("Shell output stream closed unexpectedly", e);
        }
    }

    // 发送数据到前端
    private void sendToClient(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("Failed to send message to client", e);
            }
        }
    }

    @OnClose
    public void onClose() {
        if (shellProcess != null && shellProcess.isAlive()) {
            shellProcess.destroyForcibly();
        }
    }

    @OnMessage
    @SneakyThrows
    public void OnMessage(String message) {
        try {
            // 正常输入写入 Shell 输入流
            shellInput.write(message.getBytes(StandardCharsets.UTF_8));
            shellInput.flush();
        } catch (Exception e) {
            log.error("Error writing input to shell", e);
        }
    }

    private Tuple<String[], Map> getShellCommand() {
        Map<String, String> env = new HashMap<>(System.getenv());
        String os = System.getProperty("os.name").toLowerCase();
        String[] order;
        if (os.contains("win")) {
            order = getPreferredShellCommand();
            if (order[0].equals("cmd.exe")) {
                env.put("TERM", "xterm");
            } else {
                env.put("TERM", "xterm-256color");
            }
        } else if (os.contains("linux")) {
            order = new String[]{"/bin/bash"};
        } else if (os.contains("mac")) {
            order = new String[]{"/bin/zsh"};
        } else {
            order = new String[]{"/bin/sh"};
        }
        return Tuple.newTuple(order, env);
    }

    private String[] getPreferredShellCommand() {
        // 检查 pwsh 是否存在
        if (isCommandAvailable("pwsh.exe")) {
            return new String[]{"pwsh.exe", "-NoProfile", "-NonInteractive"};
        }
        // 检查 powershell.exe 是否存在
        if (isCommandAvailable("powershell.exe")) {
            return new String[]{"powershell.exe", "-NoProfile", "-NonInteractive"};
        }
        // 默认回退到 cmd.exe
        return new String[]{"cmd.exe"};
    }

    /**
     * 判断指定的命令是否可用
     *
     * @param command 命令名称（例如 "pwsh.exe" 或 "powershell.exe"）
     * @return 如果命令可用返回 true，否则返回 false
     */
    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("where", command);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

}
