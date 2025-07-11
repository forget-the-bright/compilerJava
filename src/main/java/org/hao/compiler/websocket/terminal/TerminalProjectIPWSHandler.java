package org.hao.compiler.websocket.terminal;

import cn.hutool.core.convert.Convert;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hao.compiler.config.ws.ObjectPrincipal;
import org.hao.compiler.entity.Project;
import org.hao.compiler.service.ProjectService;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
@ServerEndpoint(value = "/terminalWS/project_ip/{projectId}") //, configurator = WsConfigurator.class 此注解相当于设置访问URL
@Slf4j
public class TerminalProjectIPWSHandler {
    private Session session;
    private Thread outputThread;
    private String clientIpAddress;
    private String projectId;

    // 会话管理集合 ip作为第一层分组, 项目id 作为第二层分组, 最后是最终的会话集合
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArraySet<TerminalProjectIPWSHandler>>> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, PtyProcess>> shellProcess = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, StringBuffer>> passwordBuffer = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> authorizedOrNot = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, MessageHistory>> lastMessage = new ConcurrentHashMap<>();

    private static CopyOnWriteArraySet<TerminalProjectIPWSHandler> getTerminalWSHandlers(String projectId, String clientIpAddress) {
        ConcurrentHashMap<String, CopyOnWriteArraySet<TerminalProjectIPWSHandler>> projectSessions = sessions.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
        CopyOnWriteArraySet<TerminalProjectIPWSHandler> terminalWSHandlers = projectSessions.computeIfAbsent(clientIpAddress, k -> new CopyOnWriteArraySet<>());
        return terminalWSHandlers;
    }

    private static ConcurrentHashMap<String, PtyProcess> getShellProcess(String projectId) {
        ConcurrentHashMap<String, PtyProcess> projectShellProcess = shellProcess.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
        return projectShellProcess;
    }

    private static StringBuffer getPasswordBuffer(String projectId, String clientIpAddress) {
        ConcurrentHashMap<String, StringBuffer> projectPasswordBuffer = passwordBuffer.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
        StringBuffer stringBuffer = projectPasswordBuffer.computeIfAbsent(clientIpAddress, k -> new StringBuffer());
        return stringBuffer;
    }

    private static ConcurrentHashMap<String, Boolean> getAuthorizedOrNot(String projectId) {
        ConcurrentHashMap<String, Boolean> projectAuthorizedOrNot = authorizedOrNot.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
        return projectAuthorizedOrNot;
    }

    private static MessageHistory getLastMessage(String projectId, String clientIpAddress) {
        ConcurrentHashMap<String, MessageHistory> projectLastMessage = lastMessage.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
        MessageHistory messageHistory = projectLastMessage.computeIfAbsent(clientIpAddress, k -> new MessageHistory(100));
        return messageHistory;
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(Session session,
                       @PathParam(value = "projectId") String projectId) {
        if (cn.hutool.core.util.StrUtil.isEmpty(projectId)) {
            session.close();
            throw new RuntimeException("项目ID不能为空");
        }
        ProjectService bean = SpringUtil.getBean(ProjectService.class);
        Project projectById = bean.getProjectById(Convert.toLong(projectId));
        if (projectById == null) {
            session.close();
            throw new RuntimeException("项目ID无效");
        }
        this.session = session;
        this.projectId = projectId;
        List<String> allIP = IPUtils.allIP;
        ObjectPrincipal<Map<String, Object>> userPrincipal = (ObjectPrincipal) session.getUserPrincipal();
        clientIpAddress = userPrincipal.getObject().get("ipAddr").toString();

        CopyOnWriteArraySet<TerminalProjectIPWSHandler> terminalWSHandlers = getTerminalWSHandlers(projectId, clientIpAddress);
        terminalWSHandlers.add(this);
        int countSum = sessions.values().stream().flatMap(map -> map.values().stream()).mapToInt(Set::size).sum();
        int clientIpProjectSum = terminalWSHandlers.size();
        log.info("有新的链接进入,当前 {} 链接总会话: {}, 当前访问ip [{}] , 项目 [{}] 会话数量: {}", this.getClass().getSimpleName(), countSum, clientIpAddress, projectId, clientIpProjectSum);

        if (allIP.contains(clientIpAddress) || isLoopbackAddress(clientIpAddress)) {//
            getAuthorizedOrNot(projectId).computeIfAbsent(clientIpAddress, k -> true);
            startShell();
        } else if (!getAuthorizedOrNot(projectId)
                .computeIfAbsent(clientIpAddress, k -> false) && clientIpProjectSum <= 1) {
            // 未授权,初始化历史消息缓存
            getPasswordBuffer(projectId, clientIpAddress);
            sendToClient(StrUtil.formatFast("IP地址【{}】未授权访问 {}", clientIpAddress, newline));
            sendToClient(StrUtil.formatFast("请输入授权密码: {}", newline));
        }
        // 同步历史消息
        if (clientIpProjectSum > 1) {
            MessageHistory messageHistory = getLastMessage(projectId, clientIpAddress);
            if (!messageHistory.isEmpty()) {
                messageHistory.consumerMessage(lastMsg -> sendMessage(lastMsg));
            } else sendMessage(StrUtil.formatFast("请按Enter键继续！！！ {}", newline));
        }
    }

    @SneakyThrows
    private synchronized void startShell() {
        PtyProcess ptyProcess = getShellProcess(projectId)
                .computeIfAbsent(clientIpAddress, k -> {
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
                MessageHistory messageHistory = getLastMessage(projectId, clientIpAddress);
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
        CopyOnWriteArraySet<TerminalProjectIPWSHandler> terminalWSHandlers = getTerminalWSHandlers(projectId, clientIpAddress);
        if (!getAuthorizedOrNot(projectId)
                .computeIfAbsent(clientIpAddress, k -> false)) {
            //存储100条历史消息
            MessageHistory messageHistory = getLastMessage(projectId, clientIpAddress);
            messageHistory.addMessage(message);
        }
        for (TerminalProjectIPWSHandler terminalWSHandler : terminalWSHandlers) {
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
        close(this, projectId, clientIpAddress);
    }

    private static synchronized void close(TerminalProjectIPWSHandler terminalWSHandler, String projectId, String clientIpAddress) {

        CopyOnWriteArraySet<TerminalProjectIPWSHandler> terminalWSHandlers = getTerminalWSHandlers(projectId, clientIpAddress);
        terminalWSHandlers.remove(terminalWSHandler);
        int countSum = sessions.values().stream().flatMap(map -> map.values().stream()).mapToInt(Set::size).sum();
        int clientIpProjectSum = terminalWSHandlers.size();
        log.info("ip [{}],项目 [{}] 的链接退出,当前 {} 链接总会话: {}, 当前退出ip会话数量: {}", clientIpAddress, projectId, terminalWSHandler.getClass().getSimpleName(), countSum, clientIpProjectSum);
        if (terminalWSHandlers.isEmpty()) {

            PtyProcess ptyProcess = getShellProcess(projectId)
                    .get(clientIpAddress);
            if (ptyProcess != null && ptyProcess.isAlive()) {
                ptyProcess.destroyForcibly();
                shellProcess
                        .computeIfAbsent(projectId, k -> new ConcurrentHashMap<>())
                        .remove(clientIpAddress);
                authorizedOrNot
                        .computeIfAbsent(projectId, k -> new ConcurrentHashMap<>())
                        .remove(clientIpAddress);
                passwordBuffer
                        .computeIfAbsent(projectId, k -> new ConcurrentHashMap<>())
                        .remove(clientIpAddress);
                lastMessage
                        .computeIfAbsent(projectId, k -> new ConcurrentHashMap<>())
                        .remove(clientIpAddress);
            }
        }
    }

    @OnMessage
    @SneakyThrows
    public void OnMessage(String message) {
        try {
            PtyProcess ptyProcess = getShellProcess(projectId).get(clientIpAddress);
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

            ConcurrentHashMap<String, Boolean> projectAuthorizedOrNot = getAuthorizedOrNot(projectId);
            StringBuffer projectPasswordBuffer = getPasswordBuffer(projectId, clientIpAddress);
            if (!projectAuthorizedOrNot.get(clientIpAddress)) {
                if (message.equals(enterCommand)) {
                    // 如果监听到回车码,进行密码验证
                    String password = projectPasswordBuffer.toString();
                    if (password.equals(terminalPassword)) {
                        // 授权成功
                        projectAuthorizedOrNot.put(clientIpAddress, true);
                        //authorizedOrNot = true;
                        sendToClient(newline);
                        sendToClient(clearCommand);
                        lastMessage
                                .computeIfAbsent(projectId, k -> new ConcurrentHashMap<>())
                                .remove(clientIpAddress);
                        startShell();
                    } else {
                        // 授权失败
                        sendToClient(newline);
                        sendToClient(clearCommand);
                        sendToClient(StrUtil.formatFast("密码错误，请重新输入: {}", newline));
                        projectPasswordBuffer.setLength(0);
                        //passwordBuffer.setLength(0);
                    }
                    return;
                } else if (message.equals(backspaceCommand) && projectPasswordBuffer.length() > 0) {
                    //如果监听到退格码 删除最后一位
                    sendToClient(sendBackspaceCommand);
                    projectPasswordBuffer.deleteCharAt(projectPasswordBuffer.length() - 1);
                    return;
                }
                projectPasswordBuffer.append(message);
                sendToClient(generateStars(message.length()));
            }
        } catch (Exception e) {
            log.error("Error writing input to shell", e);
        }
    }

}
