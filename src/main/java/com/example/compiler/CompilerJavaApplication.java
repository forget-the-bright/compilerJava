package com.example.compiler;

import com.example.compiler.config.ConsoleCapture;
import org.hao.spring.SpringRunUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CompilerJavaApplication {
    public static void main(String[] args) {
        // 启动控制台输出捕获
        ConsoleCapture.startCapture();
        
        SpringRunUtil.runAfter(CompilerJavaApplication.class, args);
        
        // 输出一些测试信息
        System.out.println("=== 应用启动完成 ===");
        System.out.println("控制台输出捕获已启用");
        System.out.println("你可以看到所有的 System.out.println() 输出");
    }
} 