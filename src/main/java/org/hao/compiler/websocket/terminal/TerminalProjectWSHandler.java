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
@ServerEndpoint(value = "/terminalWS/project/{projectId}") //, configurator = WsConfigurator.class 此注解相当于设置访问URL
@Slf4j
public class TerminalProjectWSHandler {
    private Session session;
    private Thread outputThread;
    private String projectId;

    private static final ConcurrentHashMap<String, CopyOnWriteArraySet<TerminalProjectWSHandler>> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, PtyProcess> shellProcess = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MessageHistory> lastMessage = new ConcurrentHashMap<>();


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
        String clientRemoteIp = userPrincipal.getObject().get("ipAddr").toString();
        if (!(allIP.contains(clientRemoteIp) || isLoopbackAddress(clientRemoteIp))) {
            sendToClient(StrUtil.formatFast("IP地址【{}】未授权访问 {}", clientRemoteIp, newline));
            session.close();
        }


        CopyOnWriteArraySet<TerminalProjectWSHandler> terminalIPWSHandlers = sessions.computeIfAbsent(projectId, k -> new CopyOnWriteArraySet<>());
        terminalIPWSHandlers.add(this);
        int countSum = sessions.values().stream().mapToInt(Set::size).sum();
        int clientIpSum = terminalIPWSHandlers.size();
        log.info("有新的链接进入,当前 {} 链接总会话: {}, 当前访问项目 [{}] 会话数量: {}", this.getClass().getSimpleName(), countSum, projectId, clientIpSum);
        startShell();

        // 同步历史消息
        if (clientIpSum > 1) {
            MessageHistory messageHistory = lastMessage.get(projectId);
            if (messageHistory != null) {
                messageHistory.consumerMessage(lastMsg -> sendMessage(lastMsg));
            } else sendMessage(StrUtil.formatFast("请按Enter键继续！！！ {}", newline));
        }
    }

    @SneakyThrows
    private synchronized void startShell() {
        PtyProcess ptyProcess = shellProcess.computeIfAbsent(projectId, k -> {
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
                MessageHistory messageHistory = lastMessage.computeIfAbsent(projectId, k -> new MessageHistory(100));
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
        CopyOnWriteArraySet<TerminalProjectWSHandler> terminalIPWSHandlers = sessions.get(projectId);
        for (TerminalProjectWSHandler terminalIPWSHandler : terminalIPWSHandlers) {
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
        close(this, projectId);
    }

    private static synchronized void close(TerminalProjectWSHandler terminalIPWSHandler, String projectId) {
        CopyOnWriteArraySet<TerminalProjectWSHandler> terminalIPWSHandlers = sessions.computeIfAbsent(projectId, k -> new CopyOnWriteArraySet<>());
        terminalIPWSHandlers.remove(terminalIPWSHandler);
        int countSum = sessions.values().stream().mapToInt(Set::size).sum();
        int clientIpSum = terminalIPWSHandlers.size();
        log.info("project [{}] 的链接退出,当前 {} 链接总会话: {}, 当前退出project会话数量: {}", projectId, terminalIPWSHandler.getClass().getSimpleName(), countSum, clientIpSum);
        if (terminalIPWSHandlers.isEmpty()) {
            PtyProcess ptyProcess = shellProcess.get(projectId);
            if (ptyProcess != null && ptyProcess.isAlive()) {
                ptyProcess.destroyForcibly();
                shellProcess.remove(projectId);
                lastMessage.remove(projectId);
            }
        }
    }

    @OnMessage
    @SneakyThrows
    public void OnMessage(String message) {
        try {
            PtyProcess ptyProcess = shellProcess.get(projectId);
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
            }
        } catch (Exception e) {
            log.error("Error writing input to shell", e);
        }
    }

}
