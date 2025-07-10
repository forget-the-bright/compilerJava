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
import java.util.List;
import java.util.Map;

import static org.hao.compiler.websocket.terminal.TerminalWSUtil.*;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/9 11:56
 */
@Component
@ServerEndpoint(value = "/terminalSingleWS/{SessionId}") //此注解相当于设置访问URL
@Slf4j
public class TerminalSingleWSHandler {

    private Session session;
    private PtyProcess shellProcess;
    private Thread outputThread;
    private String clientIpAddress;
    private StringBuffer passwordBuffer = new StringBuffer();
    private boolean authorizedOrNot = false;

    @OnOpen
    @SneakyThrows
    public void onOpen(Session session,
                       @PathParam(value = "SessionId") String SessionId) {
        ObjectPrincipal<Map<String, Object>> userPrincipal = (ObjectPrincipal) session.getUserPrincipal();
        clientIpAddress = userPrincipal.getObject().get("ipAddr").toString();
        this.session = session;
        List<String> allIP = IPUtils.allIP;
        if (allIP.contains(clientIpAddress) || isLoopbackAddress(clientIpAddress)) {
            authorizedOrNot = true;
            startShell();
        } else {
            sendToClient(StrUtil.formatFast("IP地址【{}】未授权访问 {}", clientIpAddress, newline));
            sendToClient(StrUtil.formatFast("请输入授权密码: {}", newline));
        }
    }

    @SneakyThrows
    private void startShell() {
        //ProcessBuilder pb;
        Tuple<String[], Map> shellCommand = getShellCommand();
        String[] cmd = shellCommand.getFirst();
        Map<String, String> env = shellCommand.getSecond();
        shellProcess = new PtyProcessBuilder().setCommand(cmd).setEnvironment(env).start();
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
                shellProcess.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
                shellProcess.getOutputStream().flush();
                return;
            }
            if (!authorizedOrNot) {
                if (message.equals(enterCommand)) {
                    String password = passwordBuffer.toString();
                    if (password.equals(terminalPassword)) {
                        authorizedOrNot = true;
                        sendToClient(newline);
                        sendToClient(clearCommand);
                        startShell();
                    } else {
                        // 授权失败
                        sendToClient(newline);
                        sendToClient(clearCommand);
                        sendToClient(StrUtil.formatFast("密码错误，请重新输入: {}", newline));
                        passwordBuffer.setLength(0);
                    }
                    return;
                } else if (message.equals(backspaceCommand) && passwordBuffer.length() > 0) {
                    //如果监听到退格码 删除最后一位
                    sendToClient(sendBackspaceCommand);
                    passwordBuffer.deleteCharAt(passwordBuffer.length() - 1);
                    return;
                }
                //sendToClient(message);
                passwordBuffer.append(message);
                sendToClient(TerminalWSUtil.generateStars(message.length()));
            }
        } catch (Exception e) {
            log.error("Error writing input to shell", e);
        }
    }
}
