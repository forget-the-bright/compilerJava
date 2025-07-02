package org.hao.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hao.compiler.config.ConsoleCapture;
import org.hao.spring.SpringRunUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class CompilerJavaApplication {
    public static void main(String[] args) {

        // 启动控制台输出捕获
        ConsoleCapture.startCapture();
        SpringRunUtil.runAfter(CompilerJavaApplication.class, args);
        // 输出一些测试信息
        log.info("=== 应用启动完成 ===");
        log.info("控制台输出捕获已启用");
        log.info("你可以看到所有的 System.out.println() 输出");
    }
} 