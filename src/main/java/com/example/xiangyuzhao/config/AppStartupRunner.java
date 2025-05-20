package com.example.xiangyuzhao.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动完成后的运行器
 * 用于在应用完全启动后执行一些初始化操作或记录日志
 */
@Component
public class AppStartupRunner implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(AppStartupRunner.class);
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    @Value("${server.port}")
    private String serverPort;

    @Override
    public void run(ApplicationArguments args) {
        logger.info("==========================================");
        logger.info("应用启动成功: {} 运行在端口: {}", applicationName, serverPort);
        logger.info("数据库初始化完成");
        logger.info("缓存系统已就绪");
        logger.info("==========================================");
        
        // 可以在这里添加其他应用启动后需要执行的逻辑
    }
} 