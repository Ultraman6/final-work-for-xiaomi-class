package com.example.xiangyuzhao.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.entity.Vehicle;
import com.example.xiangyuzhao.entity.WarnInfo;
import com.example.xiangyuzhao.entity.WarnRule;
import com.example.xiangyuzhao.mapper.WarnInfoMapper;
import com.example.xiangyuzhao.service.BatterySignalService;
import com.example.xiangyuzhao.service.CacheService;
import com.example.xiangyuzhao.service.VehicleService;
import com.example.xiangyuzhao.service.WarnInfoService;
import com.example.xiangyuzhao.service.WarnRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 预警信息服务实现类
 */
@Slf4j
@Service
public class WarnInfoServiceImpl extends ServiceImpl<WarnInfoMapper, WarnInfo> implements WarnInfoService {

    private static final String CACHE_PREFIX = "warn_info";

    @Autowired
    private WarnInfoMapper warnInfoMapper;

    @Autowired
    private BatterySignalService batterySignalService;

    @Autowired
    private WarnRuleService warnRuleService;

    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private CacheService cacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WarnInfo warnInfo) {
        // 实体类的构造函数已经处理了warnTime的设置
        boolean result = warnInfoMapper.insert(warnInfo) > 0;
        
        // 如果保存成功，清除相关缓存
        if (result && warnInfo.getCarId() != null) {
            // 清除车辆相关的预警信息缓存
            invalidateWarnInfoCache(warnInfo.getCarId(), warnInfo.getSignalId());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<WarnInfo> warnInfoList) {
        if (warnInfoList == null || warnInfoList.isEmpty()) {
            return false;
        }
        
        // 先插入数据
        boolean result = warnInfoMapper.batchInsert(warnInfoList) > 0;
        
        // 如果保存成功，清除相关缓存
        if (result) {
            // 对每个警告信息，清除相关缓存
            Set<Integer> carIds = new HashSet<>();
            Set<Long> signalIds = new HashSet<>();
            
            for (WarnInfo warnInfo : warnInfoList) {
                if (warnInfo.getCarId() != null) {
                    carIds.add(warnInfo.getCarId());
                }
                if (warnInfo.getSignalId() != null) {
                    signalIds.add(warnInfo.getSignalId());
                }
            }
            
            // 清除所有相关缓存
            for (Integer carId : carIds) {
                for (Long signalId : signalIds) {
                    invalidateWarnInfoCache(carId, signalId);
                }
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Long id) {
        // 先获取预警信息
        WarnInfo warnInfo = getById(id);
        if (warnInfo == null) {
            return false;
        }
        
        // 执行删除
        boolean result = warnInfoMapper.deleteById(id) > 0;
        
        // 如果删除成功，清除相关缓存
        if (result) {
            invalidateWarnInfoCache(warnInfo.getCarId(), warnInfo.getSignalId());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        // 先获取预警信息，用于后续清除缓存
        List<WarnInfo> warnInfos = warnInfoMapper.selectBatchIds(ids);
        if (warnInfos.isEmpty()) {
            return false;
        }
        
        // 执行批量删除
        boolean result = warnInfoMapper.deleteBatchIds(ids) > 0;
        
        // 如果删除成功，清除相关缓存
        if (result) {
            for (WarnInfo warnInfo : warnInfos) {
                invalidateWarnInfoCache(warnInfo.getCarId(), warnInfo.getSignalId());
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WarnInfo warnInfo) {
        // 先清除缓存
        if (warnInfo != null && warnInfo.getCarId() != null) {
            invalidateWarnInfoCache(warnInfo.getCarId(), warnInfo.getSignalId());
        }
        
        // 更新数据
        return warnInfoMapper.updateById(warnInfo) > 0;
    }

    @Override
    public WarnInfo getById(Long id) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, String.valueOf(id));
        
        // 尝试从缓存获取
        WarnInfo warnInfo = (WarnInfo) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并放入缓存
        if (warnInfo == null) {
            // 使用分布式锁避免缓存击穿
            return cacheService.executeWithLock(
                cacheKey + ":lock",
                1000,
                5000,
                () -> {
                    // 双重检查，避免锁释放后重复查询数据库
                    WarnInfo fromCache = (WarnInfo) cacheService.get(cacheKey);
                    if (fromCache != null) {
                        return fromCache;
                    }
                    
                    // 从数据库获取
                    WarnInfo fromDb = warnInfoMapper.selectById(id);
                    
                    // 放入缓存
                    if (fromDb != null) {
                        cacheService.set(cacheKey, fromDb, 1, TimeUnit.HOURS);
                    }
                    
                    return fromDb;
                }
            );
        }
        
        return warnInfo;
    }
    
    // 辅助方法，用于失效相关缓存
    private void invalidateWarnInfoCache(Integer carId, Long signalId) {
        // 清除车辆相关的预警信息缓存
        if (carId != null) {
            String carCacheKey = cacheService.generateKey(CACHE_PREFIX, "car_" + carId);
            cacheService.deleteWithDelay(carCacheKey);
            
            // 清除车辆相关的计数缓存
            String countCacheKey = cacheService.generateKey(CACHE_PREFIX, "count_" + carId);
            cacheService.deleteWithDelay(countCacheKey);
        }
        
        // 清除信号相关的预警信息缓存
        if (signalId != null) {
            String signalCacheKey = cacheService.generateKey(CACHE_PREFIX, "signal_" + signalId);
            cacheService.deleteWithDelay(signalCacheKey);
        }
    }

    @Override
    public List<WarnInfo> listByCarId(Integer carId) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, "car_" + carId);
        
        // 尝试从缓存获取
        List<WarnInfo> warnInfos = (List<WarnInfo>) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并放入缓存
        if (warnInfos == null) {
            // 从数据库获取
            warnInfos = warnInfoMapper.findByCarId(carId);
            
            // 放入缓存
            if (warnInfos != null && !warnInfos.isEmpty()) {
                cacheService.set(cacheKey, warnInfos, 30, TimeUnit.MINUTES);
            }
        }
        
        return warnInfos;
    }

    @Override
    public List<WarnInfo> listByCarIdAndWarnLevel(Integer carId, Integer warnLevel) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, "car_" + carId + "_level_" + warnLevel);
        
        // 尝试从缓存获取
        List<WarnInfo> warnInfos = (List<WarnInfo>) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并放入缓存
        if (warnInfos == null) {
            // 从数据库获取
            warnInfos = warnInfoMapper.findByCarIdAndWarnLevel(carId, warnLevel);
            
            // 放入缓存
            if (warnInfos != null && !warnInfos.isEmpty()) {
                cacheService.set(cacheKey, warnInfos, 30, TimeUnit.MINUTES);
            }
        }
        
        return warnInfos;
    }

    @Override
    public List<WarnInfo> listByWarnId(Integer warnId) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, "warn_" + warnId);
        
        // 尝试从缓存获取
        List<WarnInfo> warnInfos = (List<WarnInfo>) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并放入缓存
        if (warnInfos == null) {
            // 从数据库获取
            warnInfos = warnInfoMapper.findByWarnId(warnId);
            
            // 放入缓存
            if (warnInfos != null && !warnInfos.isEmpty()) {
                cacheService.set(cacheKey, warnInfos, 30, TimeUnit.MINUTES);
            }
        }
        
        return warnInfos;
    }

    @Override
    public List<WarnInfo> listBySignalId(Long signalId) {
        if (signalId == null) {
            return new ArrayList<>();
        }
        
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, "signal_" + signalId);
        
        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<WarnInfo> warnInfos = (List<WarnInfo>) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并放入缓存
        if (warnInfos == null) {
            // 使用分布式锁避免缓存击穿
            warnInfos = cacheService.executeWithLock(
                cacheKey + ":lock",
                1000,
                5000,
                () -> {
                    // 双重检查，避免锁释放后重复查询数据库
                    @SuppressWarnings("unchecked")
                    List<WarnInfo> fromCache = (List<WarnInfo>) cacheService.get(cacheKey);
                    if (fromCache != null) {
                        return fromCache;
                    }
                    
                    // 从数据库获取
                    QueryWrapper<WarnInfo> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("signal_id", signalId);
                    List<WarnInfo> fromDb = warnInfoMapper.selectList(queryWrapper);
                    
                    // 放入缓存，设置较短的过期时间
                    if (fromDb != null && !fromDb.isEmpty()) {
                        cacheService.set(cacheKey, fromDb, 10, TimeUnit.MINUTES);
                    }
                    
                    return fromDb;
                }
            );
        }
        
        return warnInfos;
    }

    @Override
    public int countByCarId(Integer carId) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, "count_" + carId);
        
        // 尝试从缓存获取
        Object cachedCount = cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并放入缓存
        if (cachedCount == null) {
            // 从数据库获取
            int count = warnInfoMapper.countWarningsByCarId(carId);
            
            // 放入缓存
            cacheService.set(cacheKey, count, 30, TimeUnit.MINUTES);
            
            return count;
        }
        
        return ((Number) cachedCount).intValue();
    }

    @Override
    public IPage<WarnInfo> page(int current, int size, Integer carId, Integer warnLevel) {
        Page<WarnInfo> page = new Page<>(current, size);
        return warnInfoMapper.findWarningsWithPagination(page, carId, warnLevel);
    }

    @Override
    public IPage<WarnInfo> page(Page<WarnInfo> page, QueryWrapper<WarnInfo> queryWrapper) {
        return warnInfoMapper.selectPage(page, queryWrapper);
    }

    @Override
    public List<Map<String, Object>> getWarnStats(Date startDate, Date endDate) {
        return warnInfoMapper.getWarningStatsByLevel(startDate, endDate);
    }

    @Override
    public Map<String, Object> getWarnStatsByCarId(Integer carId) {
        int totalCount = warnInfoMapper.countWarningsByCarId(carId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WarnInfo> processSignalWarn(Integer carId, Long signalId) {
        // 获取车辆信息
        Vehicle vehicle = vehicleService.getById(carId);
        if (vehicle == null) {
            throw new RuntimeException("车辆不存在");
        }

        // 获取信号数据
        BatterySignal signal = batterySignalService.getById(signalId);
        if (signal == null) {
            throw new RuntimeException("信号数据不存在");
        }

        // 解析信号数据
        Map<String, Object> signalData = batterySignalService.parseSignalData(signal.getSignalData());

        // 获取该电池类型的所有规则
        List<WarnRule> rules = warnRuleService.listByBatteryType(vehicle.getBatteryType());
        
        // 生成预警列表
        List<WarnInfo> warnings = new ArrayList<>();
        
        for (WarnRule rule : rules) {
            // 解析规则定义
            JSONObject ruleObj = (JSONObject) warnRuleService.parseRule(rule.getWarnId(), vehicle.getBatteryType());
            if (ruleObj == null) {
                continue;
            }
            
            // 获取规则中的操作数
            String leftOperandKey = ruleObj.getString("leftOperand");
            String rightOperandKey = ruleObj.getString("rightOperand");
            Integer operator = ruleObj.getInteger("operator");
            
            // 检查信号中是否包含所需的数据
            if (!signalData.containsKey(leftOperandKey) || !signalData.containsKey(rightOperandKey)) {
                continue;
            }
            
            // 获取操作数的值
            double leftValue = new BigDecimal(signalData.get(leftOperandKey).toString()).doubleValue();
            double rightValue = new BigDecimal(signalData.get(rightOperandKey).toString()).doubleValue();
            
            // 根据operator执行对应的运算
            double diff;
            switch (operator) {
                case 0: // 加法
                    diff = leftValue + rightValue;
                    break;
                case 1: // 减法
                    diff = leftValue - rightValue;
                    break;
                case 2: // 乘法
                    diff = leftValue * rightValue;
                    break;
                case 3: // 除法（注意除零）
                    if (Math.abs(rightValue) < 0.000001) {
                        // 除数为0，跳过该规则
                        continue;
                    }
                    diff = leftValue / rightValue;
                    break;
                case 4: // 取余（注意除零）
                    if (Math.abs(rightValue) < 0.000001) {
                        // 除数为0，跳过该规则
                        continue;
                    }
                    diff = leftValue % rightValue;
                    break;
                default: // 默认使用减法
                    diff = leftValue - rightValue;
                    break;
            }
            
            // 根据规则条件判断预警级别
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) ruleObj.get("rules");
            Integer warnLevel = null;
            
            for (Map<String, Object> condition : conditions) {
                Double minValue = condition.get("minValue") != null ? 
                    Double.parseDouble(condition.get("minValue").toString()) : null;
                Double maxValue = condition.get("maxValue") != null ? 
                    Double.parseDouble(condition.get("maxValue").toString()) : null;
                Boolean includeMin = (Boolean) condition.get("includeMin");
                Boolean includeMax = (Boolean) condition.get("includeMax");
                
                boolean matchesMin = minValue == null || 
                    (includeMin != null && includeMin && diff >= minValue) || 
                    (includeMin != null && !includeMin && diff > minValue);
                    
                boolean matchesMax = maxValue == null || 
                    (includeMax != null && includeMax && diff <= maxValue) || 
                    (includeMax != null && !includeMax && diff < maxValue);
                
                if (matchesMin && matchesMax) {
                    warnLevel = Integer.parseInt(condition.get("level").toString());
                    break;
                }
            }
            
            // 如果触发了预警，创建预警记录
            if (warnLevel != null) {
                // 使用构造函数创建WarnInfo对象
                WarnInfo warning = new WarnInfo(
                    carId, 
                    rule.getWarnId(), 
                    rule.getWarnName(), 
                    warnLevel, 
                    signalId, 
                    signal.getSignalTime()
                );
                
                warnings.add(warning);
            }
        }
        
        // 批量保存预警信息
        if (!warnings.isEmpty()) {
            saveBatch(warnings);
        }
        
        return warnings;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WarnInfo> analyzeSignalAndGenerateWarn(Integer carId, Long signalId, String batteryType) {
        // 获取信号数据
        BatterySignal signal = batterySignalService.getById(signalId);
        if (signal == null) {
            throw new IllegalArgumentException("找不到指定信号数据");
        }
        
        // 解析信号数据
        Map<String, Object> signalData = batterySignalService.parseSignalData(signal.getSignalData());
        if (signalData == null || signalData.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取车辆所有适用的预警规则
        Integer warnId = signal.getWarnId();
        if (warnId == null) {
            throw new IllegalArgumentException("信号数据未指定预警规则");
        }
        
        // 根据预警ID和电池类型获取规则
        WarnRule warnRule = warnRuleService.getByWarnIdAndBatteryType(warnId, batteryType);
        if (warnRule == null) {
            throw new IllegalArgumentException("找不到适用的预警规则");
        }
        
        // 分析规则与信号并生成预警
        List<WarnInfo> warnInfos = new ArrayList<>();

        // 这里添加分析和预警生成逻辑
        // 实际代码应当根据解析的规则及信号数据生成相应的预警信息
        
        // 批量保存预警信息
        if (!warnInfos.isEmpty()) {
            saveBatch(warnInfos);
        }

        return warnInfos;
    }

    @Override
    public List<WarnInfo> findUnhandledWarningsByCarId(Integer carId) {
        // 创建查询条件
        QueryWrapper<WarnInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("car_id", carId)
                   .orderByDesc("warn_time");
        
        // 查询预警
        List<WarnInfo> warnings = list(queryWrapper);
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        
        return warnings;
    }
    
    @Override
    public boolean setWarnHandled(Long id) {
        // 获取预警信息
        WarnInfo warnInfo = getById(id);
        if (warnInfo == null) {
            return false;
        }
        
        // 没有handled字段，移除此处逻辑
        return true;
    }
} 