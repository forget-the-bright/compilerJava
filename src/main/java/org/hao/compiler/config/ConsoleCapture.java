package org.hao.compiler.config;

import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.hao.compiler.util.SseUtil;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ConsoleCapture {
    private static final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private static final PrintStream originalOut = System.out;
    private static final PrintStream originalErr = System.err;
    
    public static void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
    }
    
    public static void startCapture() {
        // 重定向 System.out
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                originalOut.write(b);
                broadcastToEmitters(String.valueOf((char) b));
            }
            
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                originalOut.write(b, off, len);
                broadcastToEmitters(new String(b, off, len));
            }
        }));
        
        // 重定向 System.err
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                originalErr.write(b);
                broadcastToEmitters(String.valueOf((char) b));
            }
            
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                originalErr.write(b, off, len);
                broadcastToEmitters(new String(b, off, len));
            }
        }));
    }
    
    private static void broadcastToEmitters(String message) {
        for (SseEmitter emitter : emitters) {
            try {
                SseUtil.sendMegBase64(emitter, message);
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
    
    public static void stopCapture() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
} 