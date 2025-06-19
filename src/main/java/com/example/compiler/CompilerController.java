package com.example.compiler;

import cn.hutool.core.util.ReflectUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Controller
public class CompilerController {
    @GetMapping("/")
    public String editor() {
        return "editor";
    }


    @GetMapping("/compile/sse")
    @ResponseBody
    public SseEmitter compileSse(@RequestParam String code) {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                Class<?> compile = JavaCompilerService.compile(code, emitter);
                emitter.send("编译成功，开始执行...");
                Object o = ReflectUtil.newInstance(compile);
                ReflectUtil.invoke(o, "run");
                emitter.send("执行完毕！");
            } catch (Exception e) {
                try {
                    emitter.send("编译异常：" + e.getMessage());
                } catch (IOException ignored) {
                }
                e.printStackTrace();
            } finally {
                emitter.complete();
            }
        }).start();
        return emitter;
    }
} 