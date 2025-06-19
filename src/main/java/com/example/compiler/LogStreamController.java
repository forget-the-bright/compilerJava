package com.example.compiler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class LogStreamController {
    @GetMapping("/log/stream")
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(0L);
        SseLogbackAppender.addEmitter(emitter);
        ConsoleCapture.addEmitter(emitter);
        return emitter;
    }
} 