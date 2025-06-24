package org.hao.compiler.config;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/24 17:13
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // 假设我们有一个名为 'users' 的表作为标志表
        boolean tableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'PROJECT'",
                Integer.class
        ) > 0;
        boolean dataExists = jdbcTemplate.queryForObject(
                "SELECT count(1) FROM PROJECT",
                Integer.class
        ) > 0;
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
            populator.execute(jdbcTemplate.getDataSource());
        } else {
            System.out.println("Database already initialized.");
        }
    }
}
