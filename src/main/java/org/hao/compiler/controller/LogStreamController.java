package org.hao.compiler.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hao.compiler.sse.ConsoleCapture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "控制台日志")
@RestController
public class LogStreamController {
    @Operation(summary = "获取日志SSE")
    @GetMapping("/log/stream")
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(0L);
        //SseLogbackAppender.addEmitter(emitter);
        ConsoleCapture.addEmitter(emitter);
        return emitter;
    }
} 