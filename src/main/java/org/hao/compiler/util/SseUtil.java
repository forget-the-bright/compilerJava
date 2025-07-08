package org.hao.compiler.util;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/20 12:57
 */
@Slf4j
public class SseUtil {

    public static void sendMegBase64Ln(SseEmitter emitter, String msg) throws IOException {
        if (emitter == null) return;
        String encodeMsg = Base64.encode(msg + "\r\n", Charset.forName("UTF-8"));
        emitter.send(encodeMsg);
    }
    public static void sendMegBase64(SseEmitter emitter, String msg) throws IOException {
        if (emitter == null) return;
        String encodeMsg = Base64.encode(msg, Charset.forName("UTF-8"));
        emitter.send(encodeMsg);
    }
}
