package com.example.xiangyuzhao.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.entity.WarnRule;
import com.example.xiangyuzhao.mapper.WarnRuleMapper;
import com.example.xiangyuzhao.service.CacheService;
import com.example.xiangyuzhao.service.WarnRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 预警规则服务实现类
 */
@Service
public class WarnRuleServiceImpl implements WarnRuleService {

    private static final Logger logger = LoggerFactory.getLogger(WarnRuleServiceImpl.class);
    private static final String CACHE_PREFIX = "warn_rule";
    private static final String PARSED_RULE_PREFIX = "parsed_rule";

    @Autowired
    private WarnRuleMapper warnRuleMapper;
    
    @Autowired
    private CacheService cacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WarnRule warnRule) {
        // 先删除缓存
        invalidateRuleCache(warnRule.getWarnId(), warnRule.getBatteryType());
        
        // 更新数据库
        boolean result = warnRuleMapper.insert(warnRule) > 0;
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<WarnRule> warnRules) {
        if (warnRules == null || warnRules.isEmpty()) {
            return false;
        }
        
        // 批量删除缓存
        for (WarnRule rule : warnRules) {
            invalidateRuleCache(rule.getWarnId(), rule.getBatteryType());
        }
        
        // 更新数据库
        int successCount = 0;
        for (WarnRule warnRule : warnRules) {
            if (warnRuleMapper.insert(warnRule) > 0) {
                successCount++;
            }
        }
        return successCount == warnRules.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Long id) {
        WarnRule rule = getById(id);
        if (rule != null) {
            // 先删除缓存
            invalidateRuleCache(rule.getWarnId(), rule.getBatteryType());
            
            // 删除数据库记录
        return warnRuleMapper.deleteById(id) > 0;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        // 获取规则并删除相关缓存
        for (Long id : ids) {
            WarnRule rule = getById(id);
            if (rule != null) {
                invalidateRuleCache(rule.getWarnId(), rule.getBatteryType());
            }
        }
        
        // 删除数据库记录
        return warnRuleMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WarnRule warnRule) {
        // 先删除缓存
        invalidateRuleCache(warnRule.getWarnId(), warnRule.getBatteryType());
        
        // 更新数据库
        return warnRuleMapper.updateById(warnRule) > 0;
    }

    @Override
    public WarnRule getById(Long id) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, String.valueOf(id));
        
        // 尝试从缓存获取
        WarnRule warnRule = (WarnRule) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并缓存
        if (warnRule == null) {
            // 使用分布式锁避免缓存击穿
            return cacheService.executeWithLock(
                cacheKey + ":lock",
                1000,
                5000,
                () -> {
                    // 双重检查，避免锁释放后重复查询数据库
                    WarnRule fromCache = (WarnRule) cacheService.get(cacheKey);
                    if (fromCache != null) {
                        return fromCache;
                    }
                    
                    // 从数据库获取
                    WarnRule fromDb = warnRuleMapper.selectById(id);
                    
                    // 放入缓存
                    if (fromDb != null) {
                        cacheService.set(cacheKey, fromDb, 1, TimeUnit.HOURS);
                    }
                    
                    return fromDb;
                }
            );
        }
        
        return warnRule;
    }

    @Override
    public List<WarnRule> list() {
        // 列表数据不缓存，直接从数据库查询
        return warnRuleMapper.selectList(null);
    }

    @Override
    public WarnRule getByWarnIdAndBatteryType(Integer warnId, String batteryType) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, warnId + ":" + batteryType);
        
        // 尝试从缓存获取
        WarnRule warnRule = (WarnRule) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并缓存
        if (warnRule == null) {
            // 从数据库获取
            warnRule = warnRuleMapper.findByWarnIdAndBatteryType(warnId, batteryType);
            
            // 放入缓存
            if (warnRule != null) {
                cacheService.set(cacheKey, warnRule, 1, TimeUnit.HOURS);
            }
        }
        
        return warnRule;
    }

    @Override
    public List<WarnRule> listByBatteryType(String batteryType) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, "type:" + batteryType);
        
        // 尝试从缓存获取
        List<WarnRule> warnRules = (List<WarnRule>) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并缓存
        if (warnRules == null) {
            // 从数据库获取
            warnRules = warnRuleMapper.findAllRulesByBatteryType(batteryType);
            
            // 放入缓存
            if (warnRules != null && !warnRules.isEmpty()) {
                cacheService.set(cacheKey, warnRules, 1, TimeUnit.HOURS);
            }
        }
        
        return warnRules;
    }

    @Override
    public List<WarnRule> listByWarnId(Integer warnId) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, "warnId:" + warnId);
        
        // 尝试从缓存获取
        List<WarnRule> warnRules = (List<WarnRule>) cacheService.get(cacheKey);
        
        // 缓存未命中，从数据库获取并缓存
        if (warnRules == null) {
            // 从数据库获取
            warnRules = warnRuleMapper.findByWarnId(warnId);
            
            // 放入缓存
            if (warnRules != null && !warnRules.isEmpty()) {
                cacheService.set(cacheKey, warnRules, 1, TimeUnit.HOURS);
            }
        }
        
        return warnRules;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRule(Long id, String rule) {
        WarnRule warnRule = getById(id);
        if (warnRule != null) {
            // 先删除缓存
            invalidateRuleCache(warnRule.getWarnId(), warnRule.getBatteryType());
            
            // 更新规则
            warnRule.setRule(rule);
            
            // 更新数据库
            return warnRuleMapper.updateById(warnRule) > 0;
        }
        return false;
    }

    @Override
    public Object parseRule(Integer warnId, String batteryType) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(PARSED_RULE_PREFIX, warnId + ":" + batteryType);
        
        // 尝试从缓存获取解析后的规则
        Object parsedRule = cacheService.get(cacheKey);
        
        // 缓存未命中，查询原始规则并解析
        if (parsedRule == null) {
            // 查询规则
        WarnRule warnRule = getByWarnIdAndBatteryType(warnId, batteryType);
            if (warnRule == null) {
            return null;
        }
        
        try {
                // 解析规则
                parsedRule = JSON.parseObject(warnRule.getRule());
                
                // 放入缓存
                cacheService.set(cacheKey, parsedRule, 1, TimeUnit.HOURS);
        } catch (Exception e) {
                logger.error("规则解析失败: warnId={}, batteryType={}", warnId, batteryType, e);
                return null;
            }
        }
        
        return parsedRule;
    }

    @Override
    public IPage<WarnRule> page(int current, int size, String batteryType) {
        Page<WarnRule> page = new Page<>(current, size);
        
        if (batteryType != null && !batteryType.isEmpty()) {
        LambdaQueryWrapper<WarnRule> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WarnRule::getBatteryType, batteryType);
            return warnRuleMapper.selectPage(page, queryWrapper);
        } else {
            return warnRuleMapper.selectPage(page, null);
        }
    }
        
    @Override
    public IPage<WarnRule> page(Page<WarnRule> page, QueryWrapper<WarnRule> queryWrapper) {
        return warnRuleMapper.selectPage(page, queryWrapper);
    }
    
    /**
     * 使规则缓存失效
     * @param warnId 预警ID
     * @param batteryType 电池类型
     */
    private void invalidateRuleCache(Integer warnId, String batteryType) {
        if (warnId == null || batteryType == null) {
            return;
        }
        
        // 清除规则缓存
        String ruleCacheKey = cacheService.generateKey(CACHE_PREFIX, warnId + ":" + batteryType);
        cacheService.deleteWithDelay(ruleCacheKey);
        
        // 清除解析后的规则缓存
        String parsedRuleCacheKey = cacheService.generateKey(PARSED_RULE_PREFIX, warnId + ":" + batteryType);
        cacheService.deleteWithDelay(parsedRuleCacheKey);
        
        // 清除电池类型相关的规则列表缓存
        String typeCacheKey = cacheService.generateKey(CACHE_PREFIX, "type:" + batteryType);
        cacheService.deleteWithDelay(typeCacheKey);
        
        // 清除预警ID相关的规则列表缓存
        String warnIdCacheKey = cacheService.generateKey(CACHE_PREFIX, "warnId:" + warnId);
        cacheService.deleteWithDelay(warnIdCacheKey);
    }
} 