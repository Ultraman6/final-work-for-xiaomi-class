package com.example.xiangyuzhao.constant;

/**
 * Kafka相关常量
 */
public class KafkaConstants {
    
    /**
     * 电池信号处理主题
     */
    public static final String BATTERY_SIGNAL_TOPIC = "${app.kafka.battery-signal-topic}";
    
    /**
     * 消费者组ID
     */
    public static final String BATTERY_SIGNAL_GROUP = "battery-warn-group";
} 