package com.example.xiangyuzhao.service.kafka;

import com.alibaba.fastjson.JSON;
import com.example.xiangyuzhao.constant.KafkaConstants;
import com.example.xiangyuzhao.dto.kafka.BatterySignalMessage;
import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.entity.Vehicle;
import com.example.xiangyuzhao.entity.WarnInfo;
import com.example.xiangyuzhao.service.BatterySignalService;
import com.example.xiangyuzhao.service.VehicleService;
import com.example.xiangyuzhao.service.WarnInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 电池信号消息消费者
 * 多线程消费Kafka消息并确保幂等性处理
 */
@Slf4j
@Service
public class BatterySignalConsumer {

    @Autowired
    private BatterySignalService batterySignalService;
    
    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private WarnInfoService warnInfoService;

    @Value("${app.kafka.consumer.concurrency:5}")
    private int consumerThreads;

    // 记录处理中的消息ID，避免重复处理
    private final ConcurrentHashMap<Long, Long> processingMessages = new ConcurrentHashMap<>();
    
    // 添加锁保护机制，防止并发处理同一条消息
    private final ConcurrentHashMap<Long, ReentrantLock> messageLocks = new ConcurrentHashMap<>();
    
    // 处理消息的线程池
    private final ExecutorService executorService;

    public BatterySignalConsumer(@Value("${app.kafka.consumer.threads:10}") int threads) {
        // 初始化线程池
        this.executorService = Executors.newFixedThreadPool(threads);
        log.info("Battery signal consumer initialized with {} processing threads", threads);
    }

    /**
     * 批量消费电池信号消息
     * 使用手动确认模式，确保消息处理成功后再确认
     */
    @KafkaListener(
            topics = "${app.kafka.topics.battery-signal}",
            containerFactory = "batchKafkaListenerContainerFactory",
            concurrency = "${app.kafka.consumer.concurrency:5}"
    )
    public void consumeBatchMessages(
            @Payload List<BatterySignalMessage> messages,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            Acknowledgment acknowledgment) {

        if (messages.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }

        log.debug("Received batch of {} messages from Kafka", messages.size());
        
        // 1. 先串行检查每条消息的状态，过滤掉已完成处理的消息
        List<BatterySignalMessage> messagesToProcess = new ArrayList<>();
        for (BatterySignalMessage message : messages) {
            if (message == null || message.getSignalId() == null) {
                continue;
            }
            
            // 获取信号数据检查状态 - 快速过滤已处理消息
            BatterySignal signal = batterySignalService.getById(message.getSignalId());
            if (signal == null) {
                log.warn("Signal not found: {}, skipping", message.getSignalId());
                continue;
            }
            
            // 如果已经处理完成(process_time不为空)，则跳过处理
            if (signal.getProcessed() && signal.getProcessTime() != null) {
                log.debug("Signal {} already fully processed, skipping", message.getSignalId());
                continue;
            }
            
            // 如果状态不对，也跳过处理
            if (!signal.getProcessed()) {
                log.warn("Signal {} not in processing state, skipping", message.getSignalId());
                continue;
            }
            
            messagesToProcess.add(message);
        }
        
        if (messagesToProcess.isEmpty()) {
            log.info("No eligible messages to process in this batch, acknowledging");
            acknowledgment.acknowledge();
            return;
        }
        
        log.info("Processing {} eligible messages after filtering", messagesToProcess.size());
        
        // 2. 对需要处理的消息，使用异步并行处理
        List<CompletableFuture<Boolean>> futures = messagesToProcess.stream()
                .map(message -> CompletableFuture.supplyAsync(() -> processMessageWithLock(message), executorService))
                .collect(Collectors.toList());

        // 等待所有消息处理完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    long successCount = futures.stream().filter(f -> {
                        try {
                            return f.join();
                        } catch (Exception e) {
                            return false;
                        }
                    }).count();
                    
                    log.info("Successfully processed {}/{} messages in batch", successCount, messagesToProcess.size());
                    
                    // 只有在所有消息都处理成功，才确认整批消息
                    if (successCount == messagesToProcess.size()) {
                        acknowledgment.acknowledge();
                    } else {
                        // 部分失败，拒绝确认，让消息重新投递
                        log.warn("Not acknowledging batch due to {} failed messages", messagesToProcess.size() - successCount);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error processing batch messages: {}", ex.getMessage(), ex);
                    return null;
                });
    }

    /**
     * 带有锁保护的消息处理
     * 确保同一条消息不会被并发处理
     */
    private boolean processMessageWithLock(BatterySignalMessage message) {
        if (message == null || message.getSignalId() == null) {
            return false;
        }
        
        // 获取或创建该消息的锁
        ReentrantLock lock = messageLocks.computeIfAbsent(message.getSignalId(), k -> new ReentrantLock());
        
        try {
            // 尝试获取锁，如果无法获取，说明该消息正在被其他线程处理
            if (!lock.tryLock(1, TimeUnit.SECONDS)) {
                log.info("Message {} is being processed by another thread, skipping", message.getSignalId());
                return true; // 返回true表示消息处理成功，避免整批消息被拒绝
            }
            
            try {
                return processMessage(message);
            } finally {
                lock.unlock();
                // 处理完成后移除锁
                messageLocks.remove(message.getSignalId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for lock on message {}", message.getSignalId());
            return false;
        }
    }

    /**
     * 单条消息处理
     * 包含幂等性处理和并发控制
     */
    private boolean processMessage(BatterySignalMessage message) {
        if (message == null || message.getSignalId() == null) {
            log.warn("Received null or invalid message");
            return false;
        }
        
        log.debug("Processing message: {}", JSON.toJSONString(message));
        
        try {
            // 检查消息是否已经在处理中，避免重复消费
            if (isMessageProcessing(message.getSignalId())) {
                log.debug("Message {} is already being processed, skipping", message.getSignalId());
                return true; // 返回true，表示这条消息无需重新处理
            }

            log.debug("Processing message: {}", JSON.toJSONString(message));
            
            // 获取信号数据
            BatterySignal signal = batterySignalService.getById(message.getSignalId());
            if (signal == null) {
                log.error("Signal not found: {}", message.getSignalId());
                return false;
            }
            
            // 检查信号状态 - 只处理"处理中"的信号
            if (!signal.getProcessed() || (signal.getProcessed() && signal.getProcessTime() != null)) {
                // 信号未标记为处理中，或已处理完成
                log.info("Signal {} status not eligible for processing: processed={}, processTime={}", 
                        message.getSignalId(), signal.getProcessed(), signal.getProcessTime());
                return true; // 返回true以确认消息
            }
            
            // 再次检查数据库中的warn_info表，确认是否已经为此信号生成过预警
            List<WarnInfo> existingWarnings = warnInfoService.listBySignalId(message.getSignalId());
            if (!existingWarnings.isEmpty()) {
                log.info("Signal {} already has {} warnings, marking as processed", 
                      message.getSignalId(), existingWarnings.size());
                // 已经生成过预警，直接标记为处理完成
                batterySignalService.markSignalProcessed(message.getSignalId());
                return true;
            }
            
            // 获取车辆信息
            Vehicle vehicle = vehicleService.getById(message.getCarId());
            if (vehicle == null) {
                log.error("Vehicle not found: {}", message.getCarId());
                // 重置信号状态为未处理，以便重新尝试
                batterySignalService.resetSignalStatus(message.getSignalId());
                return false;
            }

            try {
                // 记录正在处理的消息
                markMessageAsProcessing(message.getSignalId());
                
                // 生成告警信息
                List<WarnInfo> warnings = warnInfoService.processSignalWarn(message.getCarId(), message.getSignalId());
                
                // 标记信号为处理完成 (processed=true, process_time=当前时间)
                batterySignalService.markSignalProcessed(message.getSignalId());
                
                log.info("Successfully processed signal {} with {} warnings", message.getSignalId(), 
                       warnings != null ? warnings.size() : 0);
                return true;
            } finally {
                // 无论成功还是失败，移除处理标志
                removeProcessingFlag(message.getSignalId());
            }
            
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            
            try {
                // 发生异常，重置状态以便重试
                batterySignalService.resetSignalStatus(message.getSignalId());
            } catch (Exception ex) {
                log.error("Failed to reset signal status: {}", ex.getMessage(), ex);
            }
            
            return false;
        }
    }

    /**
     * 标记消息为处理中
     * @param messageId 消息ID
     */
    private void markMessageAsProcessing(Long messageId) {
        if (messageId != null) {
            processingMessages.put(messageId, System.currentTimeMillis());
        }
    }

    /**
     * 检查消息是否正在处理中
     * @param messageId 消息ID
     * @return 是否正在处理
     */
    private boolean isMessageProcessing(Long messageId) {
        // 使用putIfAbsent实现原子性检查和设置
        // 如果返回null，表示消息之前不在处理中，现在已标记为处理中
        // 如果返回非null值，表示消息已在处理中
        Long timestamp = System.currentTimeMillis();
        Long previous = processingMessages.putIfAbsent(messageId, timestamp);
        
        return previous != null;
    }

    /**
     * 移除处理中标记
     */
    private void removeProcessingFlag(Long messageId) {
        processingMessages.remove(messageId);
    }
    
    /**
     * 定期清理长时间处理中的消息标记
     * 防止因异常导致的内存泄漏
     */
    @KafkaListener(
            id = "processingCleanupTrigger",
            topics = "non-existent-topic-trigger-cleanup",
            autoStartup = "false"
    )
    public void cleanupStaleProcessingFlags() {
        long currentTime = System.currentTimeMillis();
        long timeout = TimeUnit.MINUTES.toMillis(5); // 5分钟超时
        
        processingMessages.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) > timeout);
                
        // 同时清理锁映射表
        messageLocks.entrySet().removeIf(entry -> !entry.getValue().isLocked());
    }
} 