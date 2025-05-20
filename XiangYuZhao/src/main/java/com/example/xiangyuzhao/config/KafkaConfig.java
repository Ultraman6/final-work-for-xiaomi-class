package com.example.xiangyuzhao.config;

import com.alibaba.fastjson.JSON;
import com.example.xiangyuzhao.dto.kafka.BatterySignalMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka配置类
 * 配置批量监听器容器工厂和序列化器
 */
@Slf4j
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.consumer.concurrency:5}")
    private int consumerConcurrency;

    @Value("${app.kafka.topics.battery-signal}")
    private String batterySignalTopic;

    /**
     * 创建电池信号主题
     */
    @Bean
    public NewTopic batterySignalTopic() {
        return new NewTopic(batterySignalTopic, 3, (short) 1);
    }

    /**
     * 配置KafkaAdmin，用于创建主题
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * 批量消费监听容器工厂
     * 配置为手动确认模式，避免消息丢失
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BatterySignalMessage> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, BatterySignalMessage> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        
        // 设置消费者工厂
        factory.setConsumerFactory(batterySignalConsumerFactory());
        
        // 启用批量消费
        factory.setBatchListener(true);
        
        // 设置并发消费者数量
        factory.setConcurrency(consumerConcurrency);
        
        // 配置手动确认模式
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        
        return factory;
    }

    /**
     * 电池信号消费者工厂
     * 自定义反序列化器，处理JSON消息
     */
    @Bean
    public ConsumerFactory<String, BatterySignalMessage> batterySignalConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // 配置批处理参数
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024 * 100);
        
        // 使用ErrorHandlingDeserializer包装JsonDeserializer，处理反序列化异常
        return new DefaultKafkaConsumerFactory<>(
                props,
                new org.apache.kafka.common.serialization.StringDeserializer(),
                createBatterySignalDeserializer()
        );
    }

    /**
     * 创建电池信号消息的自定义反序列化器
     * 处理反序列化错误，避免单个消息错误影响整个批次
     */
    private ErrorHandlingDeserializer<BatterySignalMessage> createBatterySignalDeserializer() {
        // 委托给JsonDeserializer进行实际的反序列化
        JsonDeserializer<BatterySignalMessage> jsonDeserializer = 
                new JsonDeserializer<>(BatterySignalMessage.class);
        jsonDeserializer.setRemoveTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("com.example.xiangyuzhao.dto.kafka");
        jsonDeserializer.setUseTypeHeaders(true);
        
        // 使用ErrorHandlingDeserializer包装，捕获反序列化异常
        return new ErrorHandlingDeserializer<>(jsonDeserializer);
    }

    /**
     * 配置对象消息的生产者工厂
     */
    @Bean
    public ProducerFactory<String, Object> objectProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // 配置生产者批处理参数
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    /**
     * 配置字符串消息的生产者工厂
     */
    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 配置生产者批处理参数
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * 配置对象消息的Kafka模板
     * 主要用于发送电池信号消息
     */
    @Bean
    @Primary
    public KafkaTemplate<String, Object> objectKafkaTemplate() {
        return new KafkaTemplate<>(objectProducerFactory());
    }

    /**
     * 配置字符串消息的Kafka模板
     */
    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
} 