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
import org.hao.vo.Tuple;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

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
    private static final ConcurrentHashMap<String, CopyOnWriteArraySet<TerminalWSHandler>> sessions = new ConcurrentHashMap<>();
    private Session session;

    private static final ConcurrentHashMap<String, PtyProcess> shellProcess = new ConcurrentHashMap<>();
    // private PtyProcess shellProcess;
    // 用于读取 Shell 输出并发送给前端
    private Thread outputThread;
    private String clientIpAddress;
    private static final ConcurrentHashMap<String, StringBuffer> passwordBuffer = new ConcurrentHashMap<>();
    // private StringBuffer passwordBuffer = new StringBuffer();
    private static final ConcurrentHashMap<String, Boolean> authorizedOrNot = new ConcurrentHashMap<>();
    //private boolean authorizedOrNot = false;
    private static final ConcurrentHashMap<String, MessageHistory> lastMessage = new ConcurrentHashMap<>();

    // 清屏码
    private static final String clearCommand = "\u001B[2J\u001B[0;0f"; // ANSI escape code for clear screen
    // 退格码
    private static final String backspaceCommand = "\u007F"; // ANSI escape code for backspace
    private static final String sendBackspaceCommand = "\b \b"; // ANSI escape code for backspace
    // 回车码
    private static final String enterCommand = "\r"; // ANSI escape code for enter
    // 换行码
    private static final String newline = "\r\n"; // ANSI escape code for newLine

    @OnOpen
    @SneakyThrows
    public void onOpen(Session session,
                       @PathParam(value = "SessionId") String SessionId) {

        this.session = session;
        List<String> allIP = IPUtils.allIP;
        clientIpAddress = session.getUserProperties().get("ipAddr").toString();

        CopyOnWriteArraySet<TerminalWSHandler> terminalWSHandlers = sessions.computeIfAbsent(clientIpAddress, k -> new CopyOnWriteArraySet<>());
        terminalWSHandlers.add(this);
        int countSum = sessions.values().stream().mapToInt(Set::size).sum();
        int clientIpSum = terminalWSHandlers.size();
        log.info("有新的链接进入,当前 {} 链接总会话: {}, 当前访问ip [{}] 会话数量: {}", this.getClass().getSimpleName(), countSum, clientIpAddress, clientIpSum);

        if (allIP.contains(clientIpAddress)) {//|| isLoopbackAddress(clientIpAddress)
            authorizedOrNot.computeIfAbsent(clientIpAddress, k -> true);
            startShell();
        } else if (!authorizedOrNot.computeIfAbsent(clientIpAddress, k -> false) && clientIpSum <= 1) {
            // 未授权
            passwordBuffer.computeIfAbsent(clientIpAddress, k -> new StringBuffer());
            sendToClient(StrUtil.formatFast("IP地址【{}】未授权访问 {}", clientIpAddress, newline));
            sendToClient(StrUtil.formatFast("请输入授权密码: {}", newline));
            // return;
        }
        if (clientIpSum > 1) {
            MessageHistory messageHistory = lastMessage.get(clientIpAddress);
            if (messageHistory != null) {
                messageHistory.consumerMessage(lastMsg -> sendMessage(lastMsg));
            } else sendMessage(StrUtil.formatFast("请按Enter键继续！！！ {}", newline));
        }
    }

    @SneakyThrows
    private synchronized void startShell() {
        PtyProcess ptyProcess = shellProcess.computeIfAbsent(clientIpAddress, k -> {
            try {
                //ProcessBuilder pb;
                Tuple<String[], Map> shellCommand = getShellCommand();
                String[] cmd = shellCommand.getFirst();
                Map<String, String> env = shellCommand.getSecond();
                PtyProcess shellProcess = new PtyProcessBuilder().setCommand(cmd).setEnvironment(env).start();
                // 读取 Shell 输出流并发送给前端
                outputThread = new Thread(() -> {
                    readOutput(shellProcess);
                });
                outputThread.start();
                return shellProcess;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // 读取 Shell 输出并转发给前端
    private void readOutput(PtyProcess shellProcess) {
        try (InputStream in = shellProcess.getInputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                String data = new String(buffer, 0, read, StandardCharsets.UTF_8);
                //存储100条历史消息
                MessageHistory messageHistory = lastMessage.computeIfAbsent(clientIpAddress, k -> new MessageHistory(100));
                messageHistory.addMessage(data);
                sendToClient(data);
            }
            System.out.println("Shell output stream closed");
        } catch (IOException e) {
            log.warn("Shell output stream closed unexpectedly", e);
        }
    }

    // 发送数据到前端
    private void sendToClient(String message) {
        CopyOnWriteArraySet<TerminalWSHandler> terminalWSHandlers = sessions.get(clientIpAddress);
        if(!authorizedOrNot.computeIfAbsent(clientIpAddress, k -> false)){
            //存储100条历史消息
            MessageHistory messageHistory = lastMessage.computeIfAbsent(clientIpAddress, k -> new MessageHistory(100));
            messageHistory.addMessage(message);
        }
        for (TerminalWSHandler terminalWSHandler : terminalWSHandlers) {
            terminalWSHandler.sendMessage(message);
        }
    }

    private void sendMessage(String message) {
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
        close(this, clientIpAddress);
    }

    private static synchronized void close(TerminalWSHandler terminalWSHandler, String clientIpAddress) {
        CopyOnWriteArraySet<TerminalWSHandler> terminalWSHandlers = sessions.computeIfAbsent(clientIpAddress, k -> new CopyOnWriteArraySet<>());
        terminalWSHandlers.remove(terminalWSHandler);
        int countSum = sessions.values().stream().mapToInt(Set::size).sum();
        int clientIpSum = terminalWSHandlers.size();
        log.info("ip [{}] 的链接退出,当前 {} 链接总会话: {}, 当前退出ip会话数量: {}", clientIpAddress, terminalWSHandler.getClass().getSimpleName(), countSum, clientIpSum);
        if (terminalWSHandlers.isEmpty()) {
            PtyProcess ptyProcess = shellProcess.get(clientIpAddress);
            if (ptyProcess != null && ptyProcess.isAlive()) {
                ptyProcess.destroyForcibly();
                shellProcess.remove(clientIpAddress);
                authorizedOrNot.remove(clientIpAddress);
                passwordBuffer.remove(clientIpAddress);
                lastMessage.remove(clientIpAddress);
            }
        }
    }

    @OnMessage
    @SneakyThrows
    public void OnMessage(String message) {
        try {
            PtyProcess ptyProcess = shellProcess.get(clientIpAddress);
            if (message.contains("terminalTerm-resize")) {
                try {
                    JSONObject obj = JSON.parseObject(message);
                    int cols = obj.getIntValue("cols");
                    int rows = obj.getIntValue("rows");

                    if (ptyProcess != null && ptyProcess.isAlive()) {
                        ptyProcess.setWinSize(new WinSize(cols, rows));
                    }
                    return;
                } catch (Exception e) {
                }
            }
            if (ptyProcess != null && ptyProcess.isAlive()) {
                // 正常输入写入 Shell 输入流
                ptyProcess.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
                ptyProcess.getOutputStream().flush();
                return;
            }
            // 授权不通过情况下,进行密码验证
            if (!authorizedOrNot.get(clientIpAddress)) {
                if (message.equals(enterCommand)) {
                    // 如果监听到回车码,进行密码验证
                    String password = passwordBuffer.get(clientIpAddress).toString();
                    if (password.equals("ks125930.")) {
                        // 授权成功
                        authorizedOrNot.put(clientIpAddress, true);
                        //authorizedOrNot = true;
                        sendToClient(newline);
                        sendToClient(clearCommand);
                        lastMessage.remove(clientIpAddress);
                        startShell();
                    } else {
                        // 授权失败
                        sendToClient(newline);
                        sendToClient(clearCommand);
                        sendToClient(StrUtil.formatFast("密码错误，请重新输入: {}", newline));
                        passwordBuffer.get(clientIpAddress).setLength(0);
                        //passwordBuffer.setLength(0);
                    }
                    return;
                } else if (message.equals(backspaceCommand) && passwordBuffer.get(clientIpAddress).length() > 0) {
                    //如果监听到退格码 删除最后一位
                    sendToClient(sendBackspaceCommand);
                    passwordBuffer.get(clientIpAddress).deleteCharAt(passwordBuffer.get(clientIpAddress).length() - 1);
                    return;
                }
                passwordBuffer.get(clientIpAddress).append(message);
                sendToClient(generateStars(message.length()));
            }
        } catch (Exception e) {
            log.error("Error writing input to shell", e);
        }
    }

    private String generateStars(int length) {
        char[] stars = new char[length];
        Arrays.fill(stars, '*');

        return new String(stars);
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
