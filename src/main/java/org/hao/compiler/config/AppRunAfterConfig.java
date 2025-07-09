package org.hao.compiler.config;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.hao.aspect.LogDefineConfig;
import org.hao.compiler.config.log.ConsoleCapture;
import org.hao.compiler.config.log.UserConsoleManager;
import org.hao.core.ip.IPUtils;
import org.hao.core.print.PrintUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.util.List;

@Slf4j
@Component
public class AppRunAfterConfig implements CommandLineRunner {

    private final LogDefineConfig logDefineConfig;
    private final JdbcTemplate jdbcTemplate;
    private List<Runnable> runnableList;

    public AppRunAfterConfig(LogDefineConfig logDefineConfig, JdbcTemplate jdbcTemplate) {
        this.logDefineConfig = logDefineConfig;
        this.jdbcTemplate = jdbcTemplate;
        initRunner();
    }

    private void initRunner() {
        runnableList = ListUtil.of(
                this::databaseInitializer,
                this::setLogDefineConfig,
                this::setLogOutPut
        );
    }

    @Override
    public void run(String... args) {
        runnableList.forEach(ThreadUtil::execute);
    }

    private void setLogOutPut() {
        UserConsoleManager.initialize();
        // 启动控制台输出捕获
        ConsoleCapture.startCapture();
        // 输出一些测试信息
        log.info("=== 应用启动完成 ===");
        log.info("控制台输出捕获已启用");
    }

    private void setLogDefineConfig() {
        logDefineConfig.addBeforeMethod("default", (className, methodName, aClass) -> null);
        logDefineConfig.addAfterMethod("default", (className, methodName, aClass, error, beginIntervalMs) -> {
            Logger logger = LoggerFactory.getLogger(aClass);
            long endIntervalMs = System.currentTimeMillis();
            long intervalMs = endIntervalMs - beginIntervalMs;
            HttpServletRequest request = org.hao.core.thread.ThreadUtil.getRequest();
            String ipAddr = IPUtils.getIpAddr(request);
            String hour = LogDefineConfig.formatterHour.format(intervalMs);
            String minute = LogDefineConfig.formatterMinute.format(intervalMs);
            String second = LogDefineConfig.formatterSecond.format(intervalMs);
            boolean flag = ObjectUtil.isNotEmpty(error);
            if (flag) {
                logger.error(PrintUtil.RED.getColorStr("错误信息: {}", error.getMessage()), error);
            }
            logger.info(PrintUtil.RED.getColorStr("来自【{}】 访问 {} 类的方法: {} 执行时间： {} 小时,{} 分钟,{} 秒,共 {} 毫秒 \r\n", ipAddr, className, methodName, hour, minute, second, intervalMs));
            return null;
        });
    }

    private void databaseInitializer() {
        // 假设我们有一个名为 'users' 的表作为标志表
        boolean tableExists = ObjectUtil.defaultIfNull(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'PROJECT'",
                Integer.class
        ), 0) > 0;
        boolean dataExists = ObjectUtil.defaultIfNull(jdbcTemplate.queryForObject(
                "SELECT count(1) FROM PROJECT",
                Integer.class
        ), 0) > 0;
        if (!tableExists || !dataExists) {
            System.out.println("Database not initialized. Running initialization scripts...");

            // 运行 schema.sql 和 data.sql 初始化脚本
            // 可以直接执行 SQL 或者调用其他方式加载并执行脚本
            // 注意：这里需要根据实际需求调整
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            if (!tableExists) {
                populator.addScript(new ClassPathResource("schema.sql"));
            }
            if (!dataExists) {

                populator.addScript(new ClassPathResource("data.sql"));
            }
            DataSource dataSource = jdbcTemplate.getDataSource();
            if (dataSource != null) populator.execute(dataSource);
        } else {
            System.out.println("Database already initialized.");
        }
    }

}
