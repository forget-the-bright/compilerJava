package org.hao.compiler.config.log;

import lombok.SneakyThrows;

import java.io.*;
import java.util.function.Consumer;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/9 09:49
 */


public class UserConsoleManager {

    // 当前线程绑定的输出处理器
    private static final ThreadLocal<OutputStream> currentOutputStream = new ThreadLocal<>();
    public static final PrintStream ORIGINAL_OUT = System.out;

    /**
     * 开始拦截 System.out 输出
     *
     * @param handler 每一行输出都会调用这个 Consumer 处理
     */
    @SneakyThrows
    public static void startCapturingOutput(Consumer<String> handler) {

    }

    /**
     * 停止拦截，恢复原始 System.out
     */
    public static void stopCapturingOutput() {
        OutputStream stream = currentOutputStream.get();
        if (stream != null) {
            try {
                stream.close(); // 会触发最后 flush
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.setOut(ORIGINAL_OUT);
                currentOutputStream.remove();
            }
        }
    }

    /**
     * 强制恢复原始 System.out（可用于全局清理）
     */
    public static void restoreOriginalStreams() {
        System.setOut(ORIGINAL_OUT);
        currentOutputStream.remove();
    }

    public static void initialize() {
    }
}
