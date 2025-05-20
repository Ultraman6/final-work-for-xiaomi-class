package com.example.xiangyuzhao.service.kafka;

import com.alibaba.fastjson.JSON;
import com.example.xiangyuzhao.constant.KafkaConstants;
import com.example.xiangyuzhao.dto.kafka.BatterySignalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 电池信号消息发送服务
 */
@Slf4j
@Service
public class BatterySignalProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${app.kafka.topics.battery-signal}")
    private String batterySignalTopic;
    
    /**
     * 发送单条电池信号消息到Kafka
     * @param message 电池信号消息
     * @return 是否发送成功
     */
    public boolean sendBatterySignalMessage(BatterySignalMessage message) {
        if (message == null) {
            log.warn("Attempted to send null message");
            return false;
        }
        
        // 使用车辆ID作为消息键，保证相同车辆消息的顺序性
        String key = message.getCarId().toString();
        
        try {
            ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    batterySignalTopic, key, message);
            
            // 添加回调处理结果
            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    log.debug("Successfully sent message: {}, partition: {}, offset: {}", 
                            message, 
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
                
                @Override
                public void onFailure(Throwable ex) {
                    log.error("Failed to send message: {}, error: {}", message, ex.getMessage(), ex);
                }
            });
            
            return true;
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送单条电池信号消息到Kafka
     * 同步等待发送结果
     * @param message 电池信号消息
     * @return 是否发送成功
     */
    public boolean sendMessage(BatterySignalMessage message) {
        if (message == null) {
            log.warn("Attempted to send null message");
            return false;
        }
        
        String key = message.getCarId().toString();
        final boolean[] success = {false};
        final CountDownLatch latch = new CountDownLatch(1);
        
        try {
            ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    batterySignalTopic, key, message);
            
            // 添加回调处理结果
            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    log.debug("Successfully sent message: {}, offset: {}", 
                            message.getSignalId(), 
                            result.getRecordMetadata().offset());
                    success[0] = true;
                    latch.countDown();
                }
                
                @Override
                public void onFailure(Throwable ex) {
                    log.error("Failed to send message: {}, error: {}", 
                            message.getSignalId(), ex.getMessage());
                    latch.countDown();
                }
            });
            
            // 等待发送完成或超时
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Timeout waiting for message {} to be sent", message.getSignalId());
                return false;
            }
            
            return success[0];
        } catch (Exception e) {
            log.error("Error sending message {}: {}", message.getSignalId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批量发送电池信号消息到Kafka
     * 使用更高效的批处理方式
     * @param messages 电池信号消息列表
     * @return 发送成功的消息数量
     */
    public int sendBatchMessages(List<BatterySignalMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Attempted to send empty message batch");
            return 0;
        }
        
        log.debug("Sending batch of {} messages", messages.size());
        final CountDownLatch latch = new CountDownLatch(messages.size());
        final int[] successCount = {0};
        
        // 批量发送消息但仍然保持每条消息的单独跟踪
        for (BatterySignalMessage message : messages) {
            // 使用车辆ID作为消息键，保证相同车辆消息的顺序性
            String key = message.getCarId().toString();
            
            try {
                ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                        batterySignalTopic, key, message);
                
                // 添加回调处理结果
                future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                    @Override
                    public void onSuccess(SendResult<String, Object> result) {
                        successCount[0]++;
                        latch.countDown();
                    }
                    
                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("Failed to send message in batch: {}, error: {}", message, ex.getMessage());
                        latch.countDown();
                    }
                });
            } catch (Exception e) {
                log.error("Error sending message in batch: {}", e.getMessage(), e);
                latch.countDown();
            }
        }
        
        try {
            // 等待所有消息发送完成，或者超时退出
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Timeout waiting for batch messages to be sent");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for batch completion: {}", e.getMessage());
        }
        
        log.info("Successfully sent {}/{} messages in batch", successCount[0], messages.size());
        return successCount[0];
    }
} 