function getDemoCode() {
return `package com.example.demo;

import org.hao.core.print.PrintUtil;
import org.hao.spring.SpringRunUtil;
import org.hao.annotation.LogDefine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Greeter {
    public void run(){
        sayHello("123");
    }
    @LogDefine("123")
    public String  sayHello(String name) {
        System.out.println("Hello, " + name + "!");
        PrintUtil.BLUE.Println("name = " + name);
        SpringRunUtil.printRunInfo();
        log.info("name:{}",name);
        lombok.Lombok.preventNullAnalysis("123");
        return this.getClass().getName();
    }
}
`;
}