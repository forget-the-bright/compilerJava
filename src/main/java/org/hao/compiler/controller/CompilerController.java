package org.hao.compiler.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hao.compiler.entity.Project;
import org.hao.compiler.service.ProjectService;
import org.hao.compiler.util.SseEmitterWriter;
import org.hao.compiler.util.SseUtil;
import org.hao.core.compiler.CompilerUtil;
import org.hao.core.compiler.InMemoryClassLoader;
import org.hao.core.ip.IPUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

@Tag(name = "在线 Java 编译器")
@Controller
public class CompilerController {

    @Autowired
    private ProjectService projectService;

    @Operation(summary = "首页")
    @GetMapping("/")
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("index");
        modelAndView.addObject("title", "在线 Java 编译器");
        modelAndView.addObject("domainUrl", IPUtils.getBaseUrl());
        modelAndView.addObject("wsUrl", IPUtils.getBaseUrl().replace("http://", "ws://"));
        List<Project> projects = projectService.getProjects();
        modelAndView.addObject("projects", projects);
        modelAndView.addObject("projectSize", projects.size());
        return modelAndView;
    }

    @Operation(summary = "编辑器界面")
    @GetMapping("/editor")
    public ModelAndView editor(@RequestParam String projectId) {
        ModelAndView modelAndView = new ModelAndView("editor");
        modelAndView.addObject("title", "在线 Java 编译器");
        modelAndView.addObject("domainUrl", IPUtils.getBaseUrl());
        Project projectById = projectService.getProjectById(Convert.toLong(projectId));
        modelAndView.addObject("projectId", projectId);
        modelAndView.addObject("project", projectById);
        modelAndView.addObject("wsUrl", IPUtils.getBaseUrl().replace("http://", "ws://"));
        return modelAndView;
    }

    @Operation(summary = "编译代码SSE")
    @GetMapping("/compile/sse")
    @ResponseBody
    public SseEmitter compileSse(@RequestParam String ProjectResourceId) {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                if (StrUtil.isEmpty(ProjectResourceId)) {
                    SseUtil.sendMegBase64(emitter, "文件id为空,请选择代码文件\r\n");
                    return;
                }
                String code = projectService.getProjectSourceById(Long.parseLong(ProjectResourceId)).getContent();
                if (StrUtil.isEmpty(code)) {
                    SseUtil.sendMegBase64(emitter, "代码不能为空，请输入代码！\r\n");
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

    @Operation(summary = "编译项目代码SSE")
    @GetMapping("/compileProject/sse")
    @ResponseBody
    public SseEmitter compileProjectSse(@RequestParam String projectId) {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                if (StrUtil.isEmpty(projectId)) {
                    SseUtil.sendMegBase64(emitter, "代码不能为空，请输入代码！");
                    return;
                }
                Project projectById = projectService.getProjectById(Long.parseLong(projectId));
                if (projectById == null) {
                    SseUtil.sendMegBase64(emitter, "项目不存在！");
                    return;
                }
                if (StrUtil.isEmpty(projectById.getMainClass())) {
                    SseUtil.sendMegBase64(emitter, "项目没有主类！");
                    return;
                }
                SseEmitterWriter sseEmitterWriter = new SseEmitterWriter(emitter);
                List<String> contents = projectService.getProjectSourceContentsByProjectId(Long.parseLong(projectId));
                InMemoryClassLoader inMemoryClassLoader = CompilerUtil.compileAndLoadClass(
                        Thread.currentThread().getContextClassLoader(),
                        sseEmitterWriter,
                        contents.stream().filter(code -> StrUtil.isNotEmpty(code)).toArray(String[]::new));
                SseUtil.sendMegBase64(emitter, "编译成功，开始执行...\r\n");
                Class<?> aClass = inMemoryClassLoader.loadClass(projectById.getMainClass());
                // 定义方法名及参数类型
                String methodName = "main";
                Class<?>[] parameterTypes = new Class[]{String[].class};
                Method main = ReflectUtil.getMethod(aClass, methodName, parameterTypes);
                if (main == null) {
                    SseUtil.sendMegBase64(emitter, "项目主类没有入口函数 main！");
                    return;
                }
                ReflectUtil.invoke(null, main);
               /* Object o = ReflectUtil.newInstance(compile);
                ReflectUtil.invoke(o, "run");*/
                SseUtil.sendMegBase64(emitter, "执行完毕！\r\n");
            } catch (Exception e) {
                try {
                    SseUtil.sendMegBase64(emitter, "编译异常：" + e.getMessage() + "\r\n");
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