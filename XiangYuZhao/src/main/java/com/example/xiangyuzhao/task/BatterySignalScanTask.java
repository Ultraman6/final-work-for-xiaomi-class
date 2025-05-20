package com.example.xiangyuzhao.task;

import com.example.xiangyuzhao.dto.kafka.BatterySignalMessage;
import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.service.BatterySignalService;
import com.example.xiangyuzhao.service.kafka.BatterySignalProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 电池信号扫描定时任务
 * 简化设计：单一任务直接获取所有未处理信号，按顺序处理
 */
@Slf4j
@Component
public class BatterySignalScanTask {

    @Autowired
    private BatterySignalService batterySignalService;
    
    @Autowired
    private BatterySignalProducer batterySignalProducer;
    
    @Value("${app.scheduler.signal-batch-size:500}")
    private int batchSize;

    /**
     * 扫描所有未处理的电池信号，并单线程顺序发送到消息队列
     */
    @Scheduled(fixedDelayString = "${app.scheduler.signal-scan-delay}")
    public void scanUnprocessedSignals() {
        log.debug("Starting scan for all unprocessed battery signals");
        
        try {
            // 查询所有未处理的信号
            List<BatterySignal> allSignals = batterySignalService.findUnprocessedSignals(batchSize);
            
            if (allSignals.isEmpty()) {
                log.debug("No unprocessed signals found");
                return;
            }
            
            log.info("Found {} unprocessed signals", allSignals.size());
            
            // 使用顺序处理替代之前的多线程处理
            AtomicInteger successCount = new AtomicInteger(0);
            
            // 顺序处理每个信号
            allSignals.forEach(signal -> {
                try {
                    // 先标记为处理中状态
                    boolean marked = batterySignalService.markSignalProcessing(signal.getId());
                    if (!marked) {
                        log.debug("信号 {} 无法标记为处理中，可能已被其他进程处理", signal.getId());
                        return;
                    }
                    
                    // 构建消息
                    BatterySignalMessage message = new BatterySignalMessage(
                            signal.getId(),
                            signal.getCarId(),
                            signal.getWarnId());
                    
                    // 发送到消息队列
                    boolean sent = batterySignalProducer.sendMessage(message);
                    
                    if (sent) {
                        successCount.incrementAndGet();
                        log.debug("成功发送信号 {} 到消息队列", signal.getId());
                    } else {
                        // 发送失败，回滚状态
                        batterySignalService.resetSignalStatus(signal.getId());
                        log.warn("信号 {} 发送到消息队列失败，已重置状态", signal.getId());
                    }
                } catch (Exception e) {
                    // 发生异常，重置状态
                    batterySignalService.resetSignalStatus(signal.getId());
                    log.error("处理信号 {} 时发生异常: {}", signal.getId(), e.getMessage(), e);
                }
            });
            
            log.info("Successfully processed {} signals out of {}", successCount.get(), allSignals.size());
            
        } catch (Exception e) {
            log.error("Error processing battery signals: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 恢复被卡住的处理中信号
     * 定时检查那些标记为处理中但处理时间过长的信号
     */
    @Scheduled(fixedDelayString = "${app.scheduler.recovery-delay:60000}")
    public void recoverStuckSignals() {
        try {
            // 查找卡在处理中状态超过5分钟的信号
            List<BatterySignal> stuckSignals = batterySignalService.findStuckSignals(5);
            
            if (stuckSignals.isEmpty()) {
                return;
            }
            
            log.info("发现 {} 个卡住的信号处理任务，尝试恢复", stuckSignals.size());
            
            for (BatterySignal signal : stuckSignals) {
                boolean reset = batterySignalService.resetSignalStatus(signal.getId());
                if (reset) {
                    log.info("成功重置信号 {} 的状态为未处理", signal.getId());
                } else {
                    log.warn("重置信号 {} 的状态失败", signal.getId());
                }
            }
        } catch (Exception e) {
            log.error("恢复卡住的信号处理任务时发生异常: {}", e.getMessage(), e);
        }
    }
} 