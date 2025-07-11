package org.hao.compiler.websocket.terminal;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hao.compiler.config.ws.ObjectPrincipal;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.hao.compiler.websocket.terminal.TerminalWSUtil.*;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/9 11:56
 */
@Component
@ServerEndpoint(value = "/terminalWS/ip/{projectId}") //, configurator = WsConfigurator.class 此注解相当于设置访问URL
@Slf4j
public class TerminalIPWSHandler {
    private Session session;
    private Thread outputThread;
    private String clientIpAddress;

    private static final ConcurrentHashMap<String, CopyOnWriteArraySet<TerminalIPWSHandler>> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, PtyProcess> shellProcess = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, StringBuffer> passwordBuffer = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> authorizedOrNot = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MessageHistory> lastMessage = new ConcurrentHashMap<>();


    @OnOpen
    @SneakyThrows
    public void onOpen(Session session,
                       @PathParam(value = "projectId") String projectId) {

        this.session = session;
        List<String> allIP = IPUtils.allIP;
        ObjectPrincipal<Map<String, Object>> userPrincipal = (ObjectPrincipal) session.getUserPrincipal();
        clientIpAddress = userPrincipal.getObject().get("ipAddr").toString();

        CopyOnWriteArraySet<TerminalIPWSHandler> terminalIPWSHandlers = sessions.computeIfAbsent(clientIpAddress, k -> new CopyOnWriteArraySet<>());
        terminalIPWSHandlers.add(this);
        int countSum = sessions.values().stream().mapToInt(Set::size).sum();
        int clientIpSum = terminalIPWSHandlers.size();
        log.info("有新的链接进入,当前 {} 链接总会话: {}, 当前访问ip [{}] 会话数量: {}", this.getClass().getSimpleName(), countSum, clientIpAddress, clientIpSum);

        if (allIP.contains(clientIpAddress) || isLoopbackAddress(clientIpAddress)) {//
            authorizedOrNot.computeIfAbsent(clientIpAddress, k -> true);
            startShell();
        } else if (!authorizedOrNot.computeIfAbsent(clientIpAddress, k -> false) && clientIpSum <= 1) {
            // 未授权
            passwordBuffer.computeIfAbsent(clientIpAddress, k -> new StringBuffer());
            sendToClient(StrUtil.formatFast("IP地址【{}】未授权访问 {}", clientIpAddress, newline));
            sendToClient(StrUtil.formatFast("请输入授权密码: {}", newline));
        }
        // 同步历史消息
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
        CopyOnWriteArraySet<TerminalIPWSHandler> terminalIPWSHandlers = sessions.get(clientIpAddress);
        if (!authorizedOrNot.computeIfAbsent(clientIpAddress, k -> false)) {
            //存储100条历史消息
            MessageHistory messageHistory = lastMessage.computeIfAbsent(clientIpAddress, k -> new MessageHistory(100));
            messageHistory.addMessage(message);
        }
        for (TerminalIPWSHandler terminalIPWSHandler : terminalIPWSHandlers) {
            terminalIPWSHandler.sendMessage(message);
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

    private static synchronized void close(TerminalIPWSHandler terminalIPWSHandler, String clientIpAddress) {
        CopyOnWriteArraySet<TerminalIPWSHandler> terminalIPWSHandlers = sessions.computeIfAbsent(clientIpAddress, k -> new CopyOnWriteArraySet<>());
        terminalIPWSHandlers.remove(terminalIPWSHandler);
        int countSum = sessions.values().stream().mapToInt(Set::size).sum();
        int clientIpSum = terminalIPWSHandlers.size();
        log.info("ip [{}] 的链接退出,当前 {} 链接总会话: {}, 当前退出ip会话数量: {}", clientIpAddress, terminalIPWSHandler.getClass().getSimpleName(), countSum, clientIpSum);
        if (terminalIPWSHandlers.isEmpty()) {
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
                    if (password.equals(terminalPassword)) {
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

}
