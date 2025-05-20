package com.example.xiangyuzhao.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务
 * 实现Redis缓存操作，并保证缓存与数据库的数据一致性
 */
@Service
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    @Autowired
    @Qualifier("customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RedissonClient redissonClient;
    
    @Value("${cache.delay.deletion.millis:500}")
    private long delayDeletionMillis;
    
    @Value("${cache.default.ttl:3600}")
    private long defaultTtl;
    
    @Value("${redis.key-prefix:battery:}")
    private String keyPrefix;
    
    /**
     * 生成缓存Key
     */
    public String generateKey(String prefix, String key) {
        return keyPrefix + prefix + ":" + key;
    }
    
    /**
     * 获取缓存
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, defaultTtl, TimeUnit.SECONDS);
    }
    
    /**
     * 设置缓存（自定义过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    
    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }
    
    /**
     * 延迟双删模式实现
     * 1. 先删除缓存
     * 2. 更新数据库
     * 3. 延迟一段时间后再次删除缓存
     */
    public void deleteWithDelay(String key) {
        // 先删除缓存
        delete(key);
        
        // 异步延迟再次删除，避免缓存不一致
        deleteWithDelay(key, delayDeletionMillis);
    }
    
    /**
     * 延迟删除缓存
     */
    @Async
    public void deleteWithDelay(String key, long delayMillis) {
        try {
            Thread.sleep(delayMillis);
            delete(key);
            logger.debug("延迟删除缓存成功: {}", key);
        } catch (InterruptedException e) {
            logger.error("延迟删除缓存失败: {}", key, e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 获取分布式锁
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }
    
    /**
     * 使用分布式锁执行操作
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, java.util.function.Supplier<T> supplier) {
        RLock lock = getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            if (locked) {
                return supplier.get();
            } else {
                throw new RuntimeException("获取分布式锁失败: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取分布式锁被中断: " + lockKey, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
} 