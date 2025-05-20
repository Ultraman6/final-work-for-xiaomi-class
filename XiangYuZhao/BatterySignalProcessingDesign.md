# 电池信号处理系统设计

## 1. 概述

电池信号处理系统负责接收电池信号数据，通过规则引擎进行处理并生成预警信息。本文档重点说明系统中定时任务设计和幂等性处理机制。

## 2. 三状态模型

系统采用三状态模型管理信号处理流程，通过`processed`和`process_time`两个字段控制：

| 状态 | processed | process_time | 描述 |
|------|-----------|--------------|------|
| 未处理 | false | null | 新信号尚未处理 |
| 处理中 | true | null | 信号已标记为处理中 |
| 处理完成 | true | 非null | 信号处理已完成 |

## 3. 定时任务设计

### 3.1 信号扫描任务

`BatterySignalScanTask`负责定期扫描未处理的电池信号：

```java
@Scheduled(fixedDelayString = "${app.scheduler.signal-scan-delay}")
public void scanUnprocessedSignals() {
    // 获取未处理信号
    List<BatterySignal> allSignals = batterySignalService.findUnprocessedSignals(batchSize);
    
    // 顺序处理每个信号
    allSignals.forEach(signal -> {
        // 标记为处理中
        batterySignalService.markSignalProcessing(signal.getId());
        
        // 构建消息并发送到消息队列
        BatterySignalMessage message = new BatterySignalMessage(
                signal.getId(), signal.getCarId(), signal.getWarnId());
        batterySignalProducer.sendMessage(message);
    });
}
```

主要特点：
- 采用顺序处理而非并行处理，避免竞态条件
- 使用AtomicInteger确保计数线程安全
- 将信号状态从"未处理"转为"处理中"
- 通过消息队列解耦处理过程

### 3.2 恢复卡住信号任务

```java
@Scheduled(fixedDelayString = "${app.scheduler.recovery-delay:60000}")
public void recoverStuckSignals() {
    // 查找卡在处理中状态超过指定时间的信号
    List<BatterySignal> stuckSignals = batterySignalService.findStuckSignals(5);
    
    // 重置这些信号的状态
    for (BatterySignal signal : stuckSignals) {
        batterySignalService.resetSignalStatus(signal.getId());
    }
}
```

- 自动发现并恢复长时间处于"处理中"状态的信号
- 防止因系统错误导致信号无法完成处理

## 4. 幂等性设计

### 4.1 多层幂等保障

系统采用三层保障机制确保幂等处理：

1. **数据库层**：使用三状态模型标记处理状态
2. **内存层**：使用ConcurrentHashMap跟踪处理中的消息
3. **锁层**：使用ReentrantLock确保同一信号不被并行处理

### 4.2 内存映射对象

```java
// 记录处理中的消息
private final ConcurrentHashMap<Long, Long> processingMessages = new ConcurrentHashMap<>();

// 消息处理锁
private final ConcurrentHashMap<Long, ReentrantLock> messageLocks = new ConcurrentHashMap<>();
```

- `processingMessages`: 键为信号ID，值为处理开始时间戳
- `messageLocks`: 键为信号ID，值为该信号的专用锁

### 4.3 锁机制实现

```java
// 获取或创建锁
ReentrantLock lock = messageLocks.computeIfAbsent(message.getSignalId(), k -> new ReentrantLock());

// 尝试获取锁
if (!lock.tryLock(1, TimeUnit.SECONDS)) {
    log.info("Message {} is being processed by another thread, skipping", message.getSignalId());
    return true;
}

try {
    // 处理消息
    return processMessage(message);
} finally {
    // 释放锁并清理
    lock.unlock();
    messageLocks.remove(message.getSignalId());
}
```

- 使用tryLock避免长时间阻塞
- finally块确保锁总是会被释放

### 4.4 消费者消息预过滤

消费者接收消息后先进行预过滤：

```java
// 检查信号状态
if (signal.getProcessed() && signal.getProcessTime() != null) {
    log.debug("Signal {} already fully processed, skipping", message.getSignalId());
    continue;
}

// 检查是否已有预警记录
List<WarnInfo> existingWarnings = warnInfoService.listBySignalId(message.getSignalId());
if (!existingWarnings.isEmpty()) {
    log.info("Signal {} already has warnings, marking as processed", message.getSignalId());
    batterySignalService.markSignalProcessed(message.getSignalId());
    return true;
}
```

- 避免处理已完成的信号
- 检测已生成预警的信号，防止重复生成

## 5. 并发控制

### 5.1 任务级并发控制

- 定时任务内部使用顺序处理
- 消息消费者使用线程池并行处理不同的信号

### 5.2 信号级并发控制

- 使用messageLocks为每个信号创建独立的锁
- 确保同一信号在任何时刻只由一个线程处理

## 6. 故障恢复

### 6.1 超时恢复

- 定期检测处理超时的信号
- 重置卡住信号的状态

### 6.2 异常处理

```java
try {
    // 处理逻辑
} catch (Exception e) {
    log.error("处理信号 {} 时发生异常: {}", signal.getId(), e.getMessage(), e);
    // 重置信号状态
    batterySignalService.resetSignalStatus(signal.getId());
}
```

- 捕获异常并记录日志
- 重置信号状态允许下次重试

### 6.3 内存清理

```java
public void cleanupStaleProcessingFlags() {
    long currentTime = System.currentTimeMillis();
    long timeout = TimeUnit.MINUTES.toMillis(5);
    
    processingMessages.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > timeout);
    
    messageLocks.entrySet().removeIf(entry -> !entry.getValue().isLocked());
}
```

- 定期清理过期的处理标记和锁对象
- 防止内存泄漏

## 7. 总结

系统通过三状态模型、内存映射、锁机制和消息预过滤等多层次设计，确保了信号处理的完整性、幂等性和可恢复性，有效防止了信号重复处理和遗漏问题。 