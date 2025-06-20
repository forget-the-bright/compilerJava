package org.hao.compiler.controller;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hao.compiler.util.SseEmitterWriter;
import org.hao.compiler.util.SseUtil;
import org.hao.core.compiler.CompilerUtil;
import org.hao.core.ip.IPUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Tag(name = "在线 Java 编译器")
@Controller
public class CompilerController {
    @Operation(summary = "首页")
    @GetMapping("/")
    public ModelAndView editor() {
        ModelAndView modelAndView = new ModelAndView("editor");
        modelAndView.addObject("title", "在线 Java 编译器");
        modelAndView.addObject("domainUrl", IPUtils.getBaseUrl());
        return modelAndView;
    }

    @Operation(summary = "编译代码SSE")
    @GetMapping("/compile/sse")
    @ResponseBody
    public SseEmitter compileSse(@RequestParam String code) {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                if (StrUtil.isEmpty(code)) {
                    emitter.send("代码不能为空，请输入代码！");
                    return;
                }
                SseEmitterWriter sseEmitterWriter = new SseEmitterWriter(emitter);
                Class<?> compile = CompilerUtil.compileAndLoadClass(code, sseEmitterWriter);
                SseUtil.sendMegBase64(emitter, "编译成功，开始执行...\r\n");
                Object o = ReflectUtil.newInstance(compile);
                ReflectUtil.invoke(o, "run");
                SseUtil.sendMegBase64(emitter, "执行完毕！\r\n");
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