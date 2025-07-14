package org.hao.compiler.util;

import org.hao.compiler.process.JavaRunProcess;

import java.util.concurrent.ConcurrentHashMap;

public class CompilerLocal {
    // 使用 InheritableThreadLocal 来支持子线程继承
    private static final ConcurrentHashMap<String, JavaRunProcess> sessionHolder = new ConcurrentHashMap<>();

    /**
     * 设置当前线程的 sessionId
     */
    public static void setSessionId(String sessionId, JavaRunProcess javaRunProcess) {
        sessionHolder.put(sessionId, javaRunProcess);
    }

    /**
     * 获取当前线程的 sessionId
     */
    public static JavaRunProcess getSessionId(String sessionId) {
        return sessionHolder.get(sessionId);
    }

    /**
     * 清除当前线程的 sessionId
     */
    public static void clearSessionId(String sessionId) {
        sessionHolder.remove(sessionId);
    }

    public static void clearJavaRunProcess(JavaRunProcess javaRunProcess) {
        sessionHolder.entrySet().removeIf(entry -> entry.getValue() == javaRunProcess);
    }
}
