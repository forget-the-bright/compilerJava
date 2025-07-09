package org.hao.compiler.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hao.compiler.config.WsConfigurator;
import org.hao.core.StrUtil;
import org.hao.core.ip.IPUtils;
import org.hao.core.thread.ThreadUtil;
import org.hao.vo.Tuple;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.xml.ws.handler.MessageContext;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/9 11:56
 */
@Component
@ServerEndpoint(value = "/terminalWS/{SessionId}", configurator = WsConfigurator.class) //此注解相当于设置访问URL
@Slf4j
public class TerminalWSHandler {
    private Session session;
    private PtyProcess shellProcess;
    private OutputStream shellInput;
    // 用于读取 Shell 输出并发送给前端
    private Thread outputThread;

    private String clientIpAddress;
    private StringBuffer passwordBuffer = new StringBuffer();
    private boolean authorizedOrNot = false;

    @OnOpen
    @SneakyThrows
    public void onOpen(Session session,
                       @PathParam(value = "SessionId") String SessionId) {
        clientIpAddress = session.getUserProperties().get("ipAddr").toString();
        // log.info("来自【{}】的终端连接", clientIpAddress);
        this.session = session;
        List<String> allIP = IPUtils.allIP;
        if (allIP.contains(clientIpAddress) || isLoopbackAddress(clientIpAddress)) {
            authorizedOrNot = true;
            startShell();
        } else {
            sendToClient(StrUtil.formatFast("IP地址【{}】未授权访问 \r\n", clientIpAddress));
            sendToClient("请输入授权密码: \r\n");
        }
    }

    @SneakyThrows
    private void startShell() {
        //ProcessBuilder pb;
        Tuple<String[], Map> shellCommand = getShellCommand();
        String[] cmd = shellCommand.getFirst();
        Map<String, String> env = shellCommand.getSecond();
        shellProcess = new PtyProcessBuilder().setCommand(cmd).setEnvironment(env).start();
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
            if (message.contains("terminalTerm-resize")) {
                try {
                    JSONObject obj = JSON.parseObject(message);
                    int cols = obj.getIntValue("cols");
                    int rows = obj.getIntValue("rows");

                    if (shellProcess != null && shellProcess.isAlive()) {
                        shellProcess.setWinSize(new WinSize(cols, rows));
                    }
                    return;
                } catch (Exception e) {
                }
            }
            if (shellProcess != null && shellProcess.isAlive()) {
                // 正常输入写入 Shell 输入流
                shellInput.write(message.getBytes(StandardCharsets.UTF_8));
                shellInput.flush();
                return;
            }
            if (!authorizedOrNot) {
                if (message.equals("\r")) {
                    String password = passwordBuffer.toString();
                    if (password.equals("ks125930.")) {
                        authorizedOrNot = true;
                        startShell();
                    } else {
                        sendToClient("\r\n");
                        sendToClient("密码错误，请重新输入: \r\n");
                        passwordBuffer.setLength(0);
                    }
                    return;
                } else if (message.equals("\u007F") && passwordBuffer.length() > 0) {
                    passwordBuffer.deleteCharAt(passwordBuffer.length() - 1);
                    return;
                }
                //sendToClient(message);
                passwordBuffer.append(message);
            }
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

    public static boolean isLoopbackAddress(String ipAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            return inetAddress.isLoopbackAddress();
        } catch (UnknownHostException e) {
            // 如果IP地址格式不正确，抛出异常
            System.out.println("Invalid IP address: " + ipAddress);
            return false;
        }
    }


}
