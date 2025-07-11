package org.hao.compiler.websocket.terminal;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/10 12:32
 */


public class MessageHistory {
    private final ArrayDeque<String> history;
    private final int maxSize;

    public MessageHistory(int maxSize) {
        this.history = new ArrayDeque<>(maxSize);
        this.maxSize = maxSize;
    }

    public MessageHistory() {
        this.maxSize = 100;
        this.history = new ArrayDeque<>(this.maxSize);

    }

    // 添加新消息
    public MessageHistory addMessage(String message) {
        if (history.size() >= maxSize) {
            history.pollFirst(); // 移除最老的一条
        }
        history.addLast(message); // 添加新的到末尾
        return this;
    }

    // 获取当前保存的所有消息（按添加顺序）
    public ArrayDeque<String> getHistory() {
        return history;
    }

    public Boolean isEmpty() {
        return history.isEmpty();
    }

    public void consumerMessage(Consumer<String> consumer) {
        for (String message : history) {
            consumer.accept(message);
        }
    }
}
