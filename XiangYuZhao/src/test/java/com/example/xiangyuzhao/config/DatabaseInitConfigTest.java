package com.example.xiangyuzhao.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据库初始化配置测试类
 */
@SpringBootTest
public class DatabaseInitConfigTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DatabaseInitConfig databaseInitConfig;

    /**
     * 测试数据库表是否存在
     */
    @Test
    public void testTablesExist() {
        // 查询vehicle表记录数
        Integer vehicleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vehicle", Integer.class);
        assertTrue(vehicleCount >= 0, "vehicle表应该存在且可以查询");
        
        // 查询warn_rule表记录数
        Integer ruleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warn_rule", Integer.class);
        assertTrue(ruleCount >= 0, "warn_rule表应该存在且可以查询");
        
        // 查询battery_signal表是否存在
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'battery_signal'", 
                Integer.class);
        assertTrue(tableCount > 0, "battery_signal表应该存在");
        
        // 查询warn_info表是否存在
        tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'warn_info'", 
                Integer.class);
        assertTrue(tableCount > 0, "warn_info表应该存在");
    }
} 