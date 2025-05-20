package com.example.xiangyuzhao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 小米汽车电池预警系统应用主类
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
public class XiangYuZhaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiangYuZhaoApplication.class, args);
    }

}
