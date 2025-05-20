package com.example.xiangyuzhao.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * 数据库初始化配置
 * 用于在应用启动时检查数据库表是否存在，并根据需要初始化表结构和数据
 */
@Configuration
public class DatabaseInitConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitConfig.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private Environment environment;
    
    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            try {
                // 检查是否为开发或测试环境
                boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev") || 
                               Arrays.asList(environment.getActiveProfiles()).contains("test") ||
                               Arrays.asList(environment.getActiveProfiles()).isEmpty();
                
                logger.info("数据库配置检查开始，当前数据源URL: {}", jdbcUrl);
                
                // 检查表是否存在
                if (!tablesExist()) {
                    logger.info("数据库表不存在或不完整，开始初始化...");
                    executeScripts();
                    logger.info("数据库初始化完成");
                } else {
                    logger.info("数据库表已存在，跳过初始化");
                }
                
                // 验证表是否初始化成功
                verifyTables();
            } catch (Exception e) {
                logger.error("数据库初始化失败", e);
                throw e; // 初始化失败时终止应用程序启动
            }
        };
    }
    
    /**
     * 检查关键表是否已存在
     */
    private boolean tablesExist() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 获取当前数据库名
            String catalog = conn.getCatalog();
            String schema = conn.getSchema();
            logger.info("检查数据库: {}, schema: {}", catalog, schema);
            
            // 检查各个表是否存在
            boolean vehicleTableExists = isTableExists(metaData, "vehicle");
            boolean warnRuleTableExists = isTableExists(metaData, "warn_rule");
            boolean batterySignalTableExists = isTableExists(metaData, "battery_signal");
            boolean warnInfoTableExists = isTableExists(metaData, "warn_info");
            
            logger.info("表检查结果 - vehicle:{}, warn_rule:{}, battery_signal:{}, warn_info:{}", 
                    vehicleTableExists, warnRuleTableExists, batterySignalTableExists, warnInfoTableExists);
            
            return vehicleTableExists && warnRuleTableExists && batterySignalTableExists && warnInfoTableExists;
        } catch (SQLException e) {
            logger.error("检查数据库表出错", e);
            return false;
        }
    }
    
    /**
     * 检查指定表是否存在
     */
    private boolean isTableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
    
    /**
     * 执行初始化脚本
     */
    private void executeScripts() throws SQLException, IOException {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        
        Resource schemaScript = new ClassPathResource("schema.sql");
        Resource dataScript = new ClassPathResource("data.sql");
        
        logger.info("执行schema.sql脚本...");
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, schemaScript);
            logger.info("schema.sql执行完成");
        }
        
        logger.info("执行data.sql脚本...");
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, dataScript);
            logger.info("data.sql执行完成");
        }
    }
    
    /**
     * 验证表是否初始化成功
     */
    private void verifyTables() {
        try {
            // 检查vehicle表中的记录数
            Integer vehicleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vehicle", Integer.class);
            logger.info("vehicle表记录数: {}", vehicleCount);
            
            // 检查warn_rule表中的记录数
            Integer ruleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warn_rule", Integer.class);
            logger.info("warn_rule表记录数: {}", ruleCount);
            
        } catch (Exception e) {
            logger.warn("表验证失败，但不影响应用启动: {}", e.getMessage());
        }
    }
} 