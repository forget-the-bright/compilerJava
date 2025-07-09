package org.hao.compiler;

import org.hao.spring.SpringRunUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CompilerJavaApplication {
    public static void main(String[] args) {
        SpringRunUtil.runAfter(CompilerJavaApplication.class, args);
    }
} 