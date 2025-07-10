package org.hao.compiler.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.Writer;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/19 16:29
 */
public class SseEmitterWriter extends Writer {
    private final SseEmitter emitter;

    public SseEmitterWriter(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        String message = new String(cbuf, off, len);
        SseUtil.sendMegBase64(emitter, message);
    }

    @Override
    public void flush() throws IOException {
        // 可以根据需要实现刷新逻辑
    }

    @Override
    public void close() throws IOException {
        // 关闭资源时的处理逻辑

    }
}
