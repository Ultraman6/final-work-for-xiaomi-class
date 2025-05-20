package com.example.xiangyuzhao.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.mapper.BatterySignalMapper;
import com.example.xiangyuzhao.service.BatterySignalService;
import com.example.xiangyuzhao.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.stream.Collectors;

/**
 * 电池信号服务实现类
 */
@Slf4j
@Service
public class BatterySignalServiceImpl extends ServiceImpl<BatterySignalMapper, BatterySignal> implements BatterySignalService {

    private static final String CACHE_PREFIX = "battery_signal";

    @Autowired
    private BatterySignalMapper batterySignalMapper;
    
    @Autowired
    private CacheService cacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(BatterySignal signal) {
        // 处理信号时间为空的情况
        if (signal.getSignalTime() == null) {
            signal.setSignalTime(new Date());
        }
        
        // 保存记录
        boolean result = batterySignalMapper.insert(signal) > 0;
        
        // 清除相关缓存
        if (result) {
            cleanCache(signal.getCarId());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<BatterySignal> signals) {
        // 处理信号时间为空的情况
        for (BatterySignal signal : signals) {
            if (signal.getSignalTime() == null) {
                signal.setSignalTime(new Date());
            }
        }
        
        // 批量保存
        boolean result = super.saveBatch(signals);
        
        // 清除相关缓存
        if (result) {
            for (BatterySignal signal : signals) {
                cleanCache(signal.getCarId());
    }
        }
        
        return result;
    }

    private void cleanCache(Integer carId) {
        if (carId != null) {
            // 清除最新信号缓存
            String latestKey = cacheService.generateKey(CACHE_PREFIX, "latest_" + carId);
            cacheService.deleteWithDelay(latestKey);
            
            // 清除未处理信号缓存
            String unprocessedKey = cacheService.generateKey(CACHE_PREFIX, "unprocessed_" + carId);
            cacheService.deleteWithDelay(unprocessedKey);
        }
    }

    @Override
    public BatterySignal getById(Long id) {
        if (id == null) {
            return null;
        }
        
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, id.toString());
        
        // 尝试从缓存中获取
        BatterySignal signal = (BatterySignal) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取
        if (signal == null) {
            // 使用分布式锁防止缓存击穿
            return cacheService.executeWithLock(
                cacheKey + ":lock", 
                1000, 
                5000, 
                () -> {
                    // 双重检查
                    BatterySignal fromCache = (BatterySignal) cacheService.get(cacheKey);
                    if (fromCache != null) {
                        return fromCache;
                    }
                    
                    // 从数据库获取
                    BatterySignal fromDb = batterySignalMapper.selectById(id);
                    
                    // 将结果放入缓存
                    if (fromDb != null) {
                        cacheService.set(cacheKey, fromDb, 1, TimeUnit.HOURS);
                    } else {
                        // 缓存空结果，防止缓存穿透
                        cacheService.set(cacheKey, new BatterySignal(), 5, TimeUnit.MINUTES);
                    }
                    
                    return fromDb;
                }
            );
        }
        
        return signal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(BatterySignal signal) {
        if (signal == null || signal.getId() == null) {
            return false;
        }
        
        // 更新记录
        boolean result = batterySignalMapper.updateById(signal) > 0;
        
        // 清除缓存
        if (result) {
            // 清除信号缓存
            String cacheKey = cacheService.generateKey(CACHE_PREFIX, signal.getId().toString());
            cacheService.deleteWithDelay(cacheKey);
            
            // 清除车辆相关缓存
            cleanCache(signal.getCarId());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSignalProcessed(Long id) {
        if (id == null) {
            return false;
        }
        
        // 获取信号
        BatterySignal signal = getById(id);
        if (signal == null) {
            return false;
        }
        
        // 设置为已处理
        signal.setProcessed(true);
        signal.setProcessTime(new Date());
        
        return updateById(signal);
    }

    @Override
    public Map<String, Object> parseSignalData(String signalData) {
        if (signalData == null || signalData.isEmpty()) {
            return new HashMap<>();
    }

        try {
            // 使用FastJSON解析信号数据
            return JSON.parseObject(signalData);
        } catch (Exception e) {
            log.error("解析信号数据异常: {}", signalData, e);
            return new HashMap<>();
        }
    }

    @Override
    public BatterySignal getLatestByCarId(Integer carId) {
        if (carId == null) {
            return null;
        }
        
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, "latest_" + carId);
        
        // 尝试从缓存中获取
        BatterySignal signal = (BatterySignal) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取
        if (signal == null) {
            // 使用分布式锁防止缓存击穿
            signal = cacheService.executeWithLock(
                cacheKey + ":lock", 
                1000, 
                5000, 
                () -> {
                    // 从数据库获取
                    QueryWrapper<BatterySignal> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("car_id", carId)
                            .orderByDesc("signal_time")
                            .last("LIMIT 1");
                    
                    BatterySignal latestSignal = batterySignalMapper.selectOne(queryWrapper);
                    
                    // 将结果放入缓存，较短的过期时间
                    if (latestSignal != null) {
                        cacheService.set(cacheKey, latestSignal, 10, TimeUnit.MINUTES);
            }
                    
                    return latestSignal;
                }
            );
        }
        
        return signal;
    }

    @Override
    public IPage<BatterySignal> pageByCarId(Integer carId, Integer current, Integer size) {
        QueryWrapper<BatterySignal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("car_id", carId)
                .orderByDesc("signal_time");
        
        Page<BatterySignal> page = new Page<>(current, size);
        
        return batterySignalMapper.selectPage(page, queryWrapper);
    }

    @Override
    public List<BatterySignal> findUnprocessedSignals(Integer limit) {
        QueryWrapper<BatterySignal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("processed", false)
                .orderByAsc("signal_time")
                .last("LIMIT " + limit);
        
        return batterySignalMapper.selectList(queryWrapper);
    }

    @Override
    public List<BatterySignal> findRecentUnprocessedSignals(Integer minutes, Integer limit) {
        // 计算截止日期
        Date cutoffTime = new Date(System.currentTimeMillis() - minutes * 60 * 1000L);
        
        QueryWrapper<BatterySignal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("processed", false)
                .ge("signal_time", cutoffTime)
                .orderByAsc("signal_time")
                .last("LIMIT " + limit);
        
        return batterySignalMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cleanHistoryData(Integer carId, Date beforeDate) {
        if (carId == null || beforeDate == null) {
            return false;
        }
        
        QueryWrapper<BatterySignal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("car_id", carId)
                .lt("signal_time", beforeDate);
        
        int count = batterySignalMapper.delete(queryWrapper);
        
        // 清除缓存
        if (count > 0) {
            cleanCache(carId);
        }
        
        return count > 0;
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean removeBatchByIds(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return false;
        }
        
        // 查询要删除的记录，用于后续清除缓存
        List<BatterySignal> signals = batterySignalMapper.selectBatchIds(idList);
        
        // 删除记录
        int count = batterySignalMapper.deleteBatchIds(idList);
        
        // 清除缓存
        if (count > 0) {
            for (BatterySignal signal : signals) {
                // 清除信号缓存
                String cacheKey = cacheService.generateKey(CACHE_PREFIX, signal.getId().toString());
                cacheService.deleteWithDelay(cacheKey);
                
                // 清除车辆相关缓存
                cleanCache(signal.getCarId());
            }
        }
        
        return count > 0;
    }

    /**
     * 根据时间范围查询未处理的信号
     */
    @Override
    public List<BatterySignal> findUnprocessedSignalsByTimeRange(Date startDate, Date endDate, Integer limit) {
        if (startDate == null || endDate == null || limit == null) {
            return new ArrayList<>();
        }
        
        // 生成缓存键
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, 
                "unprocessed_range_" + startDate.getTime() + "_" + endDate.getTime() + "_" + limit);
        
        // 尝试从缓存获取结果
        @SuppressWarnings("unchecked")
        List<BatterySignal> result = (List<BatterySignal>) cacheService.get(cacheKey);
        
        if (result == null) {
            // 使用分布式锁防止缓存击穿
            result = cacheService.executeWithLock(
                    cacheKey + ":lock", 
                    500, // 较短的等待时间，避免任务堆积
                    2000, // 较短的锁定时间
                    () -> {
                        // 双重检查
                        @SuppressWarnings("unchecked")
                        List<BatterySignal> fromCache = (List<BatterySignal>) cacheService.get(cacheKey);
                        if (fromCache != null) {
                            return fromCache;
                        }
                        
                        // 构建查询
                        QueryWrapper<BatterySignal> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("processed", false)
                            .ge("signal_time", startDate)
                            .lt("signal_time", endDate)
                            .orderByAsc("signal_time") // 优先处理旧数据
                            .last("LIMIT " + limit);
                        
                        List<BatterySignal> signals = batterySignalMapper.selectList(queryWrapper);
                        
                        // 短时间缓存结果，避免频繁查询
                        if (!signals.isEmpty()) {
                            // 缓存时间短，因为数据可能会变
                            cacheService.set(cacheKey, signals, 60, TimeUnit.SECONDS);
                        }
                        
                        return signals;
                    }
            );
        }
        
        return result != null ? result : new ArrayList<>();
    }
    
    /**
     * 清理历史数据
     * 按批次删除，避免大事务
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int cleanupHistoricalDataByPartition(Date beforeDate, Integer batchSize) {
        if (beforeDate == null || batchSize == null) {
            return 0;
        }
        
        int totalDeleted = 0;
        boolean hasMore = true;
        
        // 分批次执行删除，避免长时间锁表
        while (hasMore) {
            // 先查询要删除的ID
            QueryWrapper<BatterySignal> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("id") // 只查询ID字段，减少数据传输
                    .lt("signal_time", beforeDate)
                    .orderByAsc("signal_time") // 按时间顺序删除
                    .last("LIMIT " + batchSize);
            
            List<Object> idList = batterySignalMapper.selectObjs(queryWrapper);
            
            if (idList.isEmpty()) {
                hasMore = false;
                continue;
            }
            
            // 转换为Long类型的ID列表
            List<Long> ids = idList.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toList());
            
            // 删除这一批数据
            boolean success = removeBatchByIds(ids);
            
            if (success) {
                totalDeleted += ids.size();
                log.info("Deleted {} records, total: {}", ids.size(), totalDeleted);
            } else {
                // 如果删除失败，停止循环
                hasMore = false;
                log.warn("Failed to delete batch, stopping cleanup");
            }
            
            // 如果本次获取的数量小于批次大小，说明没有更多数据了
            if (idList.size() < batchSize) {
                hasMore = false;
            }
            
            // 短暂暂停，避免连续大量删除影响系统性能
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Cleanup interrupted: {}", e.getMessage());
                hasMore = false;
            }
        }
        
        return totalDeleted;
    }

    @Override
    public boolean markSignalProcessing(Long signalId) {
        try {
            int result = baseMapper.markSignalProcessing(signalId);
            return result > 0;
        } catch (Exception e) {
            log.error("标记信号为处理中状态失败，signalId={}", signalId, e);
            return false;
        }
    }
    
    @Override
    public boolean resetSignalStatus(Long signalId) {
        try {
            int result = baseMapper.resetSignalStatus(signalId);
            return result > 0;
        } catch (Exception e) {
            log.error("重置信号状态失败，signalId={}", signalId, e);
            return false;
        }
    }
    
    @Override
    public boolean markSignalProcessed(Long signalId) {
        try {
            int result = baseMapper.markSignalProcessed(signalId);
            return result > 0;
        } catch (Exception e) {
            log.error("标记信号为处理完成失败，signalId={}", signalId, e);
            return false;
        }
    }
    
    @Override
    public List<BatterySignal> findStuckSignals(int timeoutMinutes) {
        try {
            return baseMapper.findStuckSignals(timeoutMinutes);
        } catch (Exception e) {
            log.error("查询卡在处理中状态的信号失败", e);
            return new ArrayList<>();
        }
    }
} 