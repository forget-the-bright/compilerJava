# compilerJava
---

## 📌 项目简介

`compilerJava` 是一个基于 **Spring Boot** 和 **Freemarker** 构建的 **在线 Java 代码编译与执行系统**。用户可以在浏览器中编写
Java 代码，并实时查看编译和运行结果，适用于教学、调试、轻量级编程练习等场景。

该项目集成了以下核心功能：

- 在线 Java 代码编辑器（使用 Monaco Editor）
- 实时日志输出（通过 Server-Sent Events, SSE）
- 动态编译并执行 Java 类
- 多窗口布局管理（GoldenLayout）
- 终端样式输出（xterm.js）
- 控制台重定向与日志推送（SSE + Logback Appender）

---

## 📷 截图预览
![在线 Java 编译器截图](https://github.com/user-attachments/assets/ba5cf90d-477c-401e-a649-1a8c9a3683c1)
![编译](https://github.com/user-attachments/assets/f0eeb7bd-2d4f-4980-8757-f081512f1db5)

---
## 🧩 技术栈

| 技术            | 描述                               |
|---------------|----------------------------------|
| Spring Boot   | 后端框架，提供 Web、SSE 支持               |
| Freemarker    | 前端模板引擎                           |
| Monaco Editor | 高亮显示并编辑 Java 代码                  |
| xterm.js      | 浏览器终端模拟器                         |
| GoldenLayout  | 灵活的多窗格布局管理系统                     |
| SseEmitter    | Spring 提供的 Server-Sent Events 支持 |
| Lombok        | 减少样板代码                           |
| Hutool        | 工具类库，用于编码、反射等操作                  |
| HaoUtil       | 自定义工具库                           |
| FastJSON      | JSON 序列化/反序列化                    |
| JavaParser    | 可选扩展支持：Java AST 分析               |
| Logback       | 日志框架，集成 SSE 推送                   |

---

## 📦 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── org.hao.compiler/
│   │       ├── config/                # 配置类，如控制台捕获、日志推送
│   │       ├── controller/            # REST API 控制器
│   │       ├── entity/                # 数据模型
│   │       ├── service/               # 编译服务逻辑
│   │       ├── util/                  # 工具类封装
│   │       └── CompilerJavaApplication.java # 启动类
│   │
│   ├── resources/
│       ├── static/                    # 静态资源文件（JS、CSS、图片）
│       ├── templates/                 # Freemarker 模板页面
│       ├── application.yml            # 配置文件
│       └── logback-spring.xml         # 日志配置
```

---

## 🛠️ 功能特性

### ✅ 在线代码编辑

- 使用 Monaco Editor 提供类似 VSCode 的编辑体验
- 支持 Java 语法高亮和自动缩进
- 默认加载示例 Java 代码

### ✅ 实时编译与执行

- 用户输入 Java 代码后可通过按钮触发编译与执行
- 编译错误或运行结果将实时推送到前端
- 使用动态类加载技术实现安全的运行环境

### ✅ 实时日志输出

- 所有控制台输出（System.out / System.err）被重定向并通过 SSE 推送到前端
- 使用 `SseEmitter` 实现实时通信
- 支持 Base64 编码传输，防止乱码问题

### ✅ 多窗口布局

- 使用 GoldenLayout 实现可拖拽、调整大小的面板布局
- 包含：
    - Java 编辑器（Monaco）
    - 编译输出区域
    - 实时日志终端（xterm.js）

### ✅ 安全性设计

- 使用 `SecurityManager` 可限制代码访问权限（需自行开启）
- 代码执行在独立线程中进行，避免阻塞主线程
- 支持设置最大执行时间（可扩展）

---

## 🧪 运行方式

### 1. 启动项目

- **注意事项**
- **当前项目 在线编译默认示例代码 使用的注解生成器 `lombok`, 如果你也使用注解生成器相关需要开启以下配置。没用请忽略 ！！！**
- 当前jdk版本如果大于8,jvm参数需要添加 --add-opens java.base/jdk.internal.loader=ALL-UNNAMED
- 当前jdk版本如果等于8,classpath或者jre的lib 中请添加jdk库中的 tools.jar

```bash
mvn spring-boot:run
```

或打包成 jar 文件运行：

```bash
mvn package
java -jar target/compilerJava-0.0.1-SNAPSHOT.jar
```

默认启动端口为 `8080`，访问地址为：http://localhost:8080/complier/

### 2. 访问页面

打开浏览器访问：

```
http://localhost:8080/
```

你将看到一个包含三个窗格的界面：

- 左侧：Java 代码编辑器
- 中间：编译输出
- 右侧：运行日志终端

点击“编译运行”按钮即可执行代码，并在下方看到输出结果。

---

## 🔧 核心模块介绍

### 1. [CompilerController.java](./src/main/java/org/hao/compiler/controller/CompilerController.java)

负责接收用户的 Java 代码，编译并执行，然后通过 SSE 返回执行结果。

- URL: `/compile/sse?code=...`
- 使用 [CompilerUtil.compileAndLoadClass()](https://github.com/forget-the-bright/HaoUtil/blob/main/src/main/java/org/hao/core/compiler/CompilerUtil.java#L16-L16) 编译代码
- 使用反射调用主方法执行程序

### 2. [LogStreamController.java](./src/main/java/org/hao/compiler/controller/LogStreamController.java)

提供 `/log/stream` 接口，允许前端通过 EventSource 接收服务器日志。

- 支持多个客户端同时连接
-
使用 [ConsoleCapture](./src/main/java/org/hao/compiler/config/ConsoleCapture.java#L10-L67)
或 [SseLogbackAppender](./src/main/java/org/hao/compiler/config/SseLogbackAppender.java#L13-L33)
拦截日志并广播给所有连接的客户端

### 3. [ConsoleCapture.java](./src/main/java/org/hao/compiler/config/ConsoleCapture.java)

重定向 `System.out` 和 `System.err`，将输出通过 SSE 推送给前端。

- 支持多个客户端连接
- 自动处理连接超时和关闭

### 4. [SseLogbackAppender.java](./src/main/java/org/hao/compiler/config/SseLogbackAppender.java)

自定义 Logback Appender，将日志信息通过 SSE 推送至前端。

- 可与其他 Appender 共存（如 ConsoleAppender）
- 支持多客户端连接

### 5. [editor.ftl](./src/main/resources/templates/editor.ftl)

Freemarker 主页面，整合了 Monaco Editor、xterm.js 和 GoldenLayout。

- 支持响应式布局
- 使用 JavaScript 模块化组织代码
- 支持 Base64 解码输出内容

---

## 🧪 示例代码

默认提供的示例 Java 类如下：
编译时 默认执行当前类中的 `run` 方法

```java
package com.example.demo;

import org.hao.core.print.PrintUtil;
import org.hao.spring.SpringRunUtil;
import org.hao.annotation.LogDefine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Greeter {
    public void run() {
        sayHello("123");
    }

    @LogDefine("123")
    public String sayHello(String name) {
        System.out.println("Hello, " + name + "!");
        PrintUtil.BLUE.Println("name = " + name);
        SpringRunUtil.printRunInfo();
        log.info("name:{}", name);
        return this.getClass().getName();
    }
}
```

---

## 📝 开发者指南

### 如何添加新功能？

#### 添加新的编译语言支持

只需实现一个对应的编译器类（如 `PythonCompilerService`），并在 Controller 中增加接口即可。

#### 扩展日志输出方式

你可以继承 `SseLogbackAppender` 或 `ConsoleCapture`，实现其他类型日志的推送机制（如数据库日志、异常日志等）。

#### 增加单元测试功能

可以通过解析 Java 类中的 `main()` 方法或指定运行方法名来增强灵活性。

---

## 📌 贡献指南

欢迎贡献代码！请遵循以下规范：

1. Fork 本仓库
2. 创建 feature 分支
3. 提交代码并编写清晰注释
4. 提交 PR 并说明修改内容

---

## 📄 License

MIT License

---

## 💬 联系作者

如果你有任何建议或问题，请联系邮箱：helloworlwh@163.com

---

## 🚀 后续开发计划

- ✅ 支持保存用户代码草稿
- ✅ 支持注册登录与历史记录
- ✅ 引入沙箱机制提高安全性
- ✅ 支持多种语言（Python、JavaScript、C++ 等）
- ✅ 支持远程调试与断点设置
- ✅ 支持 WebSocket 替代 SSE（双向通信）

---

## 📊 总结

`compilerJava` 是一个完整的在线 Java 编译器项目，适合用于教学演示、代码调试、面试题测试等场景。它结合了现代前后端技术，具备良好的扩展性和维护性，是一个理想的教育类或工具类开源项目。

--- 

## 感谢
- [HaoUtil](https://github.com/forget-the-bright/HaoUtil)
- [xterm.js](https://xterm.js.org/)
- [Monaco Editor](https://microsoft.github.io/monaco-editor/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Freemarker](https://freemarker.apache.org/)
- [Logback](https://logback.qos.ch/)
- [GoldenLayout](https://golden-layout.com/)
- [Java](https://www.oracle.com/java/)
- [spring-boot-websocket-lsp4j](https://github.com/lisirrx/spring-boot-websocket-lsp4j)

## 备忘导出sql 
```h2
-- 导出全部信息
SCRIPT TO 'data.sql';
-- 导出指定表信息
SCRIPT TO 'data.sql' TABLE PROJECT_RESOURCE ;
```