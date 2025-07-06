package org.hao.compiler.config;

import cn.hutool.core.util.ObjectUtil;
import org.hao.annotation.LogDefine;
import org.hao.aspect.LogDefineConfig;
import org.hao.core.print.PrintUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import org.springframework.stereotype.Component;

@Component
public class ConfigCommandLineRunner implements CommandLineRunner {

    @Autowired
    private LogDefineConfig logDefineConfig;

    @Override
    public void run(String... args) throws Exception {
        logDefineConfig.addBeforeMethod("default", (className, methodName, aClass) -> {

            return null;
        });
        logDefineConfig.addAfterMethod("default", (className, methodName, aClass, error, beginIntervalMs) -> {
            Logger logger = LoggerFactory.getLogger(aClass);
            long endIntervalMs = System.currentTimeMillis();
            long intervalMs = endIntervalMs - beginIntervalMs;
            String hour = LogDefineConfig.formatterHour.format(intervalMs);
            String minute = LogDefineConfig.formatterMinute.format(intervalMs);
            String second = LogDefineConfig.formatterSecond.format(intervalMs);
            boolean flag = ObjectUtil.isNotEmpty(error);
            if (flag) {
                logger.error(PrintUtil.RED.getColorStr("错误信息: " + error.getMessage()), error);
            }
            logger.info(PrintUtil.RED.getColorStr(className + "类的方法:" + methodName + "执行时间： " + hour + "小时," + minute + "分钟," + second + "秒,共" + intervalMs + "毫秒"));
            return null;
        });
    }
}
