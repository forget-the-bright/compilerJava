package com.example.compiler.config;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import cn.hutool.core.codec.Base64;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SseLogbackAppender extends AppenderBase<ILoggingEvent> {
    private static final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public static void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
    }

    @Override
    protected void append(ILoggingEvent event) {
        String msg = event.getFormattedMessage();
        for (SseEmitter emitter : emitters) {
            try {
                String encodeMsg = Base64.encode(msg, Charset.forName("UTF-8"));
                emitter.send(encodeMsg);
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
} 