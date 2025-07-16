package org.hao.compiler.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hao.annotation.LogDefine;
import org.hao.compiler.entity.Project;
import org.hao.compiler.entity.User;
import org.hao.compiler.process.JavaRunProcess;
import org.hao.compiler.service.UserService;
import org.hao.compiler.service.impl.ProjectService;
import org.hao.compiler.sse.SseEmitterWriter;
import org.hao.compiler.sse.SseUtil;
import org.hao.compiler.util.CompilerLocal;
import org.hao.core.compiler.CompilerUtil;
import org.hao.core.compiler.InMemoryClassLoader;
import org.hao.core.ip.IPUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "在线 Java 编译器")
@Controller
public class CompilerController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;


    @Operation(summary = "首页")
    @GetMapping("/")
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("index");
        List<Project> projects = projectService.getProjects();
        modelAndView.addObject("projects", projects);
        modelAndView.addObject("projectSize", projects.size());
        return modelAndView;
    }

    @Operation(summary = "编辑器界面")
    @GetMapping("/editor")
    public ModelAndView editor(@RequestParam String projectId) {
        ModelAndView modelAndView = new ModelAndView("editor");
        Project projectById = projectService.getProjectById(Convert.toLong(projectId));
        modelAndView.addObject("projectId", projectId);
        modelAndView.addObject("project", projectById);
        modelAndView.addObject("SessionId", UUID.randomUUID().toString());
        return modelAndView;
    }

    @Operation(summary = "登录页面")
    @GetMapping("/login")
    public ModelAndView login() {
        if (StpUtil.isLogin()) {
            ModelAndView modelAndView = new ModelAndView("redirect:/");
            return modelAndView;
        }
        ModelAndView modelAndView = new ModelAndView("login");
        return modelAndView;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    @LogDefine(description = "用户登录")
    public String handleLogin(
            @RequestParam String username,
            @RequestParam String password,
            Model model) {

        if (StpUtil.isLogin()) {
            return "redirect:/";
        }
        User user = userService.getUserByName(username);
        if (user == null) {
            model.addAttribute("error", "用户不存在");
            return "login"; // 登录失败，返回登录页面
        }
        String hashPassword = userService.hashPassword(password, user.getPasswordSalt());
        if (!hashPassword.equals(user.getPasswordHash())) {
            model.addAttribute("error", "密码错误");
            return "login"; // 登录失败，返回登录页面
        }
        user.setLastLoginTime(new Date()).updateById();
        // 第二步：根据账号id，进行登录
        try {
            StpUtil.login(username);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "login"; // 登录失败，返回登录页面
        }
        StpUtil.getSession(true).set("user", user);
        // 登录成功，跳转主页
        return "redirect:/";
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public String handleLogin() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        return "redirect:/";
    }

    @GetMapping("/register")
    public ModelAndView handleRegister() {
        if (StpUtil.isLogin()) {
            ModelAndView modelAndView = new ModelAndView("redirect:/");
            return modelAndView;
        }
        ModelAndView modelAndView = new ModelAndView("register");
        return modelAndView;
    }

    @PostMapping("/register")
    @LogDefine(description = "用户注册")
    public String handleRegister(
            @RequestParam("user_name") String userName,
            @RequestParam("nick_name") String nickName,
            @RequestParam("email") String email,
            @RequestParam("mobile_number") String mobileNumber,
            @RequestParam("password") String password,
            @RequestParam("confirm_password") String confirmPassword,
            Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "两次密码不一致");
            return "register";
        }

        if (userName == null || userName.trim().isEmpty()) {
            model.addAttribute("error", "用户名不能为空");
            return "register";
        }

        try {
            userService.register(userName, nickName, email, mobileNumber, password);
        } catch (Exception e) {
            model.addAttribute("error", "注册失败：" + e.getMessage());
            return "register";
        }

        return "redirect:/login";
    }

    @Operation(summary = "编译代码SSE")
    @GetMapping("/compile/sse")
    @ResponseBody
    public SseEmitter compileSse(@RequestParam String ProjectResourceId) {
        SseEmitter emitter = new SseEmitter();
        new Thread(() -> {
            try {
                if (StrUtil.isEmpty(ProjectResourceId)) {
                    SseUtil.sendMegBase64Ln(emitter, "文件id为空,请选择代码文件");
                    return;
                }
                String code = projectService.getProjectSourceById(Long.parseLong(ProjectResourceId)).getContent();
                if (StrUtil.isEmpty(code)) {
                    SseUtil.sendMegBase64Ln(emitter, "代码不能为空,请输入代码");
                    return;
                }
                SseEmitterWriter sseEmitterWriter = new SseEmitterWriter(emitter);
                SseUtil.sendMegBase64Ln(emitter, "正在编译...");
                Class<?> compile = CompilerUtil.compileAndLoadClass(code, sseEmitterWriter);
                SseUtil.sendMegBase64Ln(emitter, "编译成功,开始执行...");
                Method run = ReflectUtil.getMethod(compile, "run");
                Method main = ReflectUtil.getMethod(compile, "main", new Class[]{String[].class});
                if (run == null && main == null) {
                    sseEmitterWriter.write("当前类没有入口函数请添加静态main函数 或者 对象run方法");
                }
                Object obj = ReflectUtil.newInstance(compile);
                ReflectUtil.invoke(obj, ObjectUtil.defaultIfNull(main, run));
                SseUtil.sendMegBase64Ln(emitter, "执行完毕！");
            } catch (Exception e) {
                try {
                    SseUtil.sendMegBase64Ln(emitter, "编译异常：" + e.getMessage());
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
                    SseUtil.sendMegBase64Ln(emitter, "代码不能为空,请输入代码！");
                    return;
                }
                Project projectById = projectService.getProjectById(Long.parseLong(projectId));
                if (projectById == null) {
                    SseUtil.sendMegBase64Ln(emitter, "项目不存在！");
                    return;
                }
                if (StrUtil.isEmpty(projectById.getMainClass())) {
                    SseUtil.sendMegBase64Ln(emitter, "项目没有主类！");
                    return;
                }
                SseEmitterWriter sseEmitterWriter = new SseEmitterWriter(emitter);
                List<String> contents = projectService.getProjectSourceContentsByProjectId(Long.parseLong(projectId));
                SseUtil.sendMegBase64Ln(emitter, "正在编译...");
                InMemoryClassLoader inMemoryClassLoader = CompilerUtil.compileAndLoadClass(
                        Thread.currentThread().getContextClassLoader(),
                        sseEmitterWriter,
                        contents.stream().filter(code -> StrUtil.isNotEmpty(code)).toArray(String[]::new));
                SseUtil.sendMegBase64Ln(emitter, "编译成功,开始执行...");
                Class<?> aClass = inMemoryClassLoader.loadClass(projectById.getMainClass());
                // 定义方法名及参数类型
                String methodName = "main";
                Class<?>[] parameterTypes = new Class[]{String[].class};
                Method main = ReflectUtil.getMethod(aClass, methodName, parameterTypes);
                if (main == null) {
                    SseUtil.sendMegBase64Ln(emitter, "项目主类没有入口函数 main！");
                    return;
                }
                ReflectUtil.invoke(null, main);
                SseUtil.sendMegBase64Ln(emitter, "执行完毕！");
            } catch (Exception e) {
                try {
                    SseUtil.sendMegBase64Ln(emitter, "编译异常：" + e.getMessage() + "");
                } catch (IOException ignored) {
                }
                e.printStackTrace();
            } finally {
                emitter.complete();
            }
        }).start();
        return emitter;
    }


    @Operation(summary = "编译项目代码本地SSE")
    @GetMapping("/compileProjectLocal/sse")
    @ResponseBody
    public SseEmitter compileProjectLocalSse(@RequestParam String projectId, @RequestParam(required = false) String SessionId) {
        SseEmitter emitter = new SseEmitter();
        ThreadUtil.execAsync(() -> {
            try {
                SseUtil.sendMegBase64Ln(emitter, "正在校验编译输入信息...");
                if (StrUtil.isEmpty(projectId)) {
                    SseUtil.sendMegBase64Ln(emitter, "项目id不能为空,请输入代码！");
                    return;
                }
                Project projectById = projectService.getProjectById(Long.parseLong(projectId));
                if (projectById == null) {
                    SseUtil.sendMegBase64Ln(emitter, "项目id对应的不存在！");
                    return;
                }
                if (StrUtil.isEmpty(projectById.getMainClass())) {
                    SseUtil.sendMegBase64Ln(emitter, "项目没有设置主类！");
                    return;
                }
                SseUtil.sendMegBase64Ln(emitter, "编译相关信息初始化...");
                //编译信息初始化。
                String outPutDir = StrUtil.format("./compile_output/project_{}/", projectId);
                String mainClass = projectById.getMainClass();
                JavaRunProcess javaRunProcess = new JavaRunProcess(outPutDir, mainClass, emitter);
                CompilerLocal.setSessionId(SessionId, javaRunProcess);
                emitter.onCompletion(() -> {
                    javaRunProcess.destroyForcibly();
                    CompilerLocal.clearSessionId(SessionId);
                });
                SseUtil.sendMegBase64Ln(emitter, "编译准备,获取项目源代码...");
                //编译准备,获取项目源代码
                SseEmitterWriter sseEmitterWriter = new SseEmitterWriter(emitter);
                List<String> contents = projectService.getProjectSourceContentsByProjectId(Long.parseLong(projectId));
                SseUtil.sendMegBase64Ln(emitter, "执行编译,编译中...");
                //执行编译
                CompilerUtil.compileToLocalFile(
                        outPutDir,
                        sseEmitterWriter,
                        contents
                                .stream()
                                .filter(code -> StrUtil.isNotEmpty(code)).toArray(String[]::new));
                SseUtil.sendMegBase64Ln(emitter, "编译成功,开始执行...");
                //执行运行编译结果class
                javaRunProcess.run();
            } catch (Exception e) {
                try {
                    SseUtil.sendMegBase64Ln(emitter, "编译异常：" + e.getMessage() + "");
                } catch (Exception exception) {
                }
                log.error("编译异常", e);
                emitter.complete();
                CompilerLocal.clearSessionId(SessionId);
            }
        }, true);
        return emitter;
    }

    @Operation(summary = "编译项目代码本地SSE")
    @GetMapping("/compileProjectLocal/stop")
    @ResponseBody
    public void compileProjectLocalStop(@RequestParam String SessionId) {
        JavaRunProcess sessionId = CompilerLocal.getSessionId(SessionId);
        if (sessionId == null) return;
        sessionId.dynamicDestory();
    }

} 