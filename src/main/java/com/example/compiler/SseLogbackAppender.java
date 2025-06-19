package com.example.compiler;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
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
               // msg=  msg.replace("\n", "\\n");
               // msg= msg.replace("\r", "\\r");
              //  msg= msg.replace("\t", "\\t");
                emitter.send(msg+"\n");
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
} 