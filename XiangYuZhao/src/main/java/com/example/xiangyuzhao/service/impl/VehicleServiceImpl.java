package com.example.xiangyuzhao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.entity.Vehicle;
import com.example.xiangyuzhao.mapper.VehicleMapper;
import com.example.xiangyuzhao.service.CacheService;
import com.example.xiangyuzhao.service.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 车辆信息服务实现类
 */
@Service
public class VehicleServiceImpl implements VehicleService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleServiceImpl.class);
    private static final String CACHE_PREFIX = "vehicle";

    @Autowired
    private VehicleMapper vehicleMapper;
    
    @Autowired
    private CacheService cacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Vehicle vehicle) {
        // 先删除缓存，避免缓存不一致
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, String.valueOf(vehicle.getCarId()));
        cacheService.deleteWithDelay(cacheKey);
        
        // 如果没有设置VID，自动生成
        if (vehicle.getVid() == null || vehicle.getVid().isEmpty()) {
            vehicle.setVid(generateRandomVid());
        }
        
        // 初始化默认值
        if (vehicle.getMileage() == null) {
            vehicle.setMileage(new BigDecimal("0.00"));
        }
        
        if (vehicle.getBatteryHealth() == null) {
            vehicle.setBatteryHealth(new BigDecimal("100.00"));
        }
        
        // 更新数据库
        boolean result = vehicleMapper.insert(vehicle) > 0;
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<Vehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return false;
        }
        
        // 处理每个车辆对象，设置默认值
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getVid() == null || vehicle.getVid().isEmpty()) {
                vehicle.setVid(generateRandomVid());
            }
            
            if (vehicle.getMileage() == null) {
                vehicle.setMileage(new BigDecimal("0.00"));
            }
            
            if (vehicle.getBatteryHealth() == null) {
                vehicle.setBatteryHealth(new BigDecimal("100.00"));
            }
        }
        
        // 循环插入每个车辆
        int successCount = 0;
        for (Vehicle vehicle : vehicles) {
            if (vehicleMapper.insert(vehicle) > 0) {
                successCount++;
            }
        }
        
        return successCount == vehicles.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Integer carId) {
        // 先删除缓存，避免缓存不一致
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, String.valueOf(carId));
        cacheService.deleteWithDelay(cacheKey);
        
        // 删除数据库记录
        boolean result = vehicleMapper.deleteById(carId) > 0;
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBatchByIds(List<Integer> carIds) {
        if (carIds == null || carIds.isEmpty()) {
            return false;
        }
        return vehicleMapper.deleteBatchIds(carIds) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Vehicle vehicle) {
        // 先删除缓存，避免缓存不一致
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, String.valueOf(vehicle.getCarId()));
        cacheService.deleteWithDelay(cacheKey);
        
        // 更新数据库
        boolean result = vehicleMapper.updateById(vehicle) > 0;
        
        return result;
    }

    @Override
    public Vehicle getById(Integer carId) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, String.valueOf(carId));
        
        // 尝试从缓存获取数据
        Vehicle vehicle = (Vehicle) cacheService.get(cacheKey);
        
        // 如果缓存未命中，则从数据库获取并放入缓存
        if (vehicle == null) {
            // 使用分布式锁避免缓存击穿
            return cacheService.executeWithLock(
                cacheKey + ":lock",
                1000,
                5000,
                () -> {
                    // 双重检查，避免锁释放后重复查询数据库
                    Vehicle fromCache = (Vehicle) cacheService.get(cacheKey);
                    if (fromCache != null) {
                        return fromCache;
                    }
                    
                    // 从数据库获取数据
                    Vehicle fromDb = vehicleMapper.selectById(carId);
                    
                    // 放入缓存
                    if (fromDb != null) {
                        cacheService.set(cacheKey, fromDb, 1, TimeUnit.HOURS);
                    }
                    
                    return fromDb;
                }
            );
        }
        
        return vehicle;
    }

    @Override
    public Vehicle getByVid(String vid) {
        // 生成缓存key
        String cacheKey = cacheService.generateKey(CACHE_PREFIX + ":vid", vid);
        
        // 尝试从缓存获取数据
        Vehicle vehicle = (Vehicle) cacheService.get(cacheKey);
        
        // 如果缓存未命中，则从数据库获取并放入缓存
        if (vehicle == null) {
            // 从数据库获取数据
            vehicle = vehicleMapper.findByVid(vid);
            
            // 放入缓存
            if (vehicle != null) {
                cacheService.set(cacheKey, vehicle, 1, TimeUnit.HOURS);
            }
        }
        
        return vehicle;
    }

    @Override
    public IPage<Vehicle> page(long page, long size) {
        Page<Vehicle> pageParam = new Page<>(page, size);
        return vehicleMapper.selectVehiclePage(pageParam);
    }

    @Override
    public IPage<Vehicle> page(long page, long size, QueryWrapper<Vehicle> queryWrapper) {
        Page<Vehicle> pageParam = new Page<>(page, size);
        return vehicleMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public List<Vehicle> listByBatteryType(String batteryType) {
        // 这里不使用缓存，因为列表可能会很大，且变动频繁
        return vehicleMapper.findByBatteryType(batteryType);
    }

    @Override
    public List<Vehicle> listByIds(List<Integer> carIds) {
        if (carIds == null || carIds.isEmpty()) {
            return new ArrayList<>();
        }
        return vehicleMapper.selectBatchIds(carIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatteryHealth(Integer carId, BigDecimal batteryHealth) {
        // 先删除缓存，避免缓存不一致
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, String.valueOf(carId));
        cacheService.deleteWithDelay(cacheKey);
        
        // 更新数据库
        boolean result = vehicleMapper.updateBatteryHealth(carId, batteryHealth) > 0;
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMileage(Integer carId, BigDecimal mileage) {
        // 先删除缓存，避免缓存不一致
        String cacheKey = cacheService.generateKey(CACHE_PREFIX, String.valueOf(carId));
        cacheService.deleteWithDelay(cacheKey);
        
        // 更新数据库
        boolean result = vehicleMapper.updateMileage(carId, mileage) > 0;
        
        return result;
    }

    @Override
    public String generateRandomVid() {
        // 生成16位随机字符串作为VID
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder vid = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 16; i++) {
            vid.append(characters.charAt(random.nextInt(characters.length())));
        }
        
        return vid.toString();
    }
} 