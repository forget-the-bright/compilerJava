package org.hao.compiler.websocket.terminal;

import org.hao.vo.Tuple;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/10 13:59
 */
public class TerminalWSUtil {

    // 清屏码
    public static final String clearCommand = "\u001B[2J\u001B[0;0f"; // ANSI escape code for clear screen
    // 退格码
    public static final String backspaceCommand = "\u007F"; // ANSI escape code for backspace
    public static final String sendBackspaceCommand = "\b \b"; // ANSI escape code for backspace
    // 回车码
    public static final String enterCommand = "\r"; // ANSI escape code for enter
    // 换行码
    public static final String newline = "\r\n"; // ANSI escape code for newLine

    public static final String terminalPassword = "ks125930.";

    public static String generateStars(int length) {
        char[] stars = new char[length];
        Arrays.fill(stars, '*');
        return new String(stars);
    }

    public static Tuple<String[], Map> getShellCommand() {
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

    public static String[] getPreferredShellCommand() {
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
    public static boolean isCommandAvailable(String command) {
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
