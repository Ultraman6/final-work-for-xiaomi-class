package com.example.xiangyuzhao.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VehicleService单元测试
 * 全面覆盖所有方法和核心功能
 */
@SpringBootTest
public class VehicleServiceTest {

    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private CacheService cacheService;
    
    private String testVid;
    private Integer testCarId;
    
    /**
     * 测试前准备工作
     */
    @BeforeEach
    public void setUp() {
        // 生成唯一的车辆标识
        testVid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        // 创建测试车辆
        Vehicle vehicle = new Vehicle();
        vehicle.setVid(testVid);
        vehicle.setBatteryType("三元电池");
        vehicle.setMileage(new BigDecimal("10000.00"));
        vehicle.setBatteryHealth(new BigDecimal("0.95"));
        
        vehicleService.save(vehicle);
        testCarId = vehicle.getCarId();
    }
    
    /**
     * 测试保存车辆
     */
    @Test
    @Transactional
    public void testSave() {
        // 创建新的测试车辆，确保VID不重复
        String uniqueVid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Vehicle vehicle = new Vehicle();
        vehicle.setVid(uniqueVid);
        vehicle.setBatteryType("铁锂电池");
        vehicle.setMileage(new BigDecimal("5000.00"));
        vehicle.setBatteryHealth(new BigDecimal("0.98"));
        
        // 保存
        boolean result = vehicleService.save(vehicle);
        
        // 验证
        assertTrue(result, "保存车辆应该成功");
        assertNotNull(vehicle.getCarId(), "车辆ID不应为空");
        
        // 查询并验证
        Vehicle found = vehicleService.getById(vehicle.getCarId());
        assertNotNull(found, "应能查询到保存的车辆");
        assertEquals(uniqueVid, found.getVid(), "VID应一致");
        assertEquals("铁锂电池", found.getBatteryType(), "电池类型应一致");
        assertEquals(0, new BigDecimal("5000.00").compareTo(found.getMileage()), "里程应一致");
    }
    
    /**
     * 测试重复保存相同VID的车辆 - 测试唯一约束
     */
    @Test
    @Transactional
    public void testSaveDuplicateVid() {
        // 创建与setUp中相同VID的车辆
        Vehicle vehicle = new Vehicle();
        vehicle.setVid(testVid); // 使用相同的VID
        vehicle.setBatteryType("三元电池");
        
        // 验证是否抛出异常或返回失败
        try {
            boolean result = vehicleService.save(vehicle);
            // 如果没有抛出异常，验证结果应为false
            assertFalse(result, "保存重复VID的车辆应该失败");
        } catch (DuplicateKeyException e) {
            // 如果抛出唯一约束异常，则测试通过
            assertTrue(true, "保存重复VID的车辆应该抛出异常");
        }
    }
    
    /**
     * 测试批量保存车辆
     */
    @Test
    @Transactional
    public void testSaveBatch() {
        List<Vehicle> vehicles = new ArrayList<>();
        
        // 创建多辆测试车辆
        for (int i = 0; i < 5; i++) {
            String uniqueVid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            Vehicle vehicle = new Vehicle();
            vehicle.setVid(uniqueVid);
            vehicle.setBatteryType(i % 2 == 0 ? "三元电池" : "铁锂电池");
            vehicle.setMileage(new BigDecimal(1000 * (i + 1)));
            vehicle.setBatteryHealth(new BigDecimal("0." + (90 + i)));
            vehicles.add(vehicle);
        }
        
        // 批量保存
        boolean result = vehicleService.saveBatch(vehicles);
        
        // 验证
        assertTrue(result, "批量保存车辆应该成功");
        
        // 验证每辆车都有ID
        for (Vehicle vehicle : vehicles) {
            assertNotNull(vehicle.getCarId(), "每辆车都应有ID");
        }
        
        // 验证是否所有车辆都已保存
        List<Integer> carIds = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            carIds.add(vehicle.getCarId());
        }
        
        List<Vehicle> savedVehicles = vehicleService.listByIds(carIds);
        assertEquals(vehicles.size(), savedVehicles.size(), "应保存所有车辆");
    }
    
    /**
     * 测试根据ID查询车辆
     */
    @Test
    @Transactional
    public void testGetById() {
        // 查询setUp中创建的车辆
        Vehicle found = vehicleService.getById(testCarId);
        
        // 验证
        assertNotNull(found, "应能查询到车辆");
        assertEquals(testCarId, found.getCarId(), "车辆ID应一致");
        assertEquals(testVid, found.getVid(), "VID应一致");
        assertEquals("三元电池", found.getBatteryType(), "电池类型应一致");
    }
    
    /**
     * 测试根据VID查询车辆
     */
    @Test
    @Transactional
    public void testGetByVid() {
        // 查询setUp中创建的车辆
        Vehicle found = vehicleService.getByVid(testVid);
        
        // 验证
        assertNotNull(found, "应能通过VID查询到车辆");
        assertEquals(testCarId, found.getCarId(), "车辆ID应一致");
        assertEquals(testVid, found.getVid(), "VID应一致");
        assertEquals("三元电池", found.getBatteryType(), "电池类型应一致");
    }
    
    /**
     * 测试查询不存在的VID
     */
    @Test
    @Transactional
    public void testGetByNonExistentVid() {
        Vehicle found = vehicleService.getByVid("不存在的VID");
        assertNull(found, "查询不存在的VID应返回null");
    }
    
    /**
     * 测试根据ID删除车辆
     */
    @Test
    @Transactional
    public void testRemoveById() {
        // 删除setUp中创建的车辆
        boolean result = vehicleService.removeById(testCarId);
        
        // 验证
        assertTrue(result, "删除车辆应该成功");
        assertNull(vehicleService.getById(testCarId), "删除后应查询不到车辆");
        
        // 验证缓存是否已清除
        // 生成缓存key，使用相同的方式
        String cacheKey = "vehicle:" + testCarId;
        assertNull(cacheService.get(cacheKey), "缓存应已清除");
    }
    
    /**
     * 测试更新车辆信息
     */
    @Test
    @Transactional
    public void testUpdateById() {
        // 更新setUp中创建的车辆
        Vehicle vehicle = vehicleService.getById(testCarId);
        assertNotNull(vehicle, "应能查询到车辆");
        
        // 修改车辆信息
        vehicle.setBatteryType("磷酸铁锂电池");
        vehicle.setMileage(new BigDecimal("15000.00"));
        vehicle.setBatteryHealth(new BigDecimal("0.85"));
        
        // 更新
        boolean result = vehicleService.updateById(vehicle);
        
        // 验证
        assertTrue(result, "更新车辆应该成功");
        
        // 查询更新后的车辆
        Vehicle updated = vehicleService.getById(testCarId);
        assertNotNull(updated, "应能查询到更新后的车辆");
        assertEquals("磷酸铁锂电池", updated.getBatteryType(), "电池类型应已更新");
        assertEquals(0, new BigDecimal("15000.00").compareTo(updated.getMileage()), "里程应已更新");
        assertEquals(0, new BigDecimal("0.85").compareTo(updated.getBatteryHealth()), "电池健康度应已更新");
    }

    
    /**
     * 测试根据电池类型查询车辆列表
     */
    @Test
    @Transactional
    public void testListByBatteryType() {
        // 添加多辆不同电池类型的车辆
        int count1 = 3; // 三元电池车辆数
        int count2 = 4; // 铁锂电池车辆数
        
        // 添加三元电池车辆
        for (int i = 0; i < count1; i++) {
            String uniqueVid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            Vehicle vehicle = new Vehicle();
            vehicle.setVid(uniqueVid);
            vehicle.setBatteryType("三元电池");
            vehicleService.save(vehicle);
        }
        
        // 添加铁锂电池车辆
        for (int i = 0; i < count2; i++) {
            String uniqueVid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            Vehicle vehicle = new Vehicle();
            vehicle.setVid(uniqueVid);
            vehicle.setBatteryType("铁锂电池");
            vehicleService.save(vehicle);
        }
        
        // 查询三元电池车辆
        List<Vehicle> ncmVehicles = vehicleService.listByBatteryType("三元电池");
        
        // 验证
        assertNotNull(ncmVehicles, "三元电池车辆列表不应为null");
        assertTrue(ncmVehicles.size() >= count1 + 1, "三元电池车辆数量应至少为" + (count1 + 1)); // +1是因为setUp中创建的车辆
        
        // 验证电池类型
        for (Vehicle vehicle : ncmVehicles) {
            assertEquals("三元电池", vehicle.getBatteryType(), "车辆电池类型应为三元电池");
        }
        
        // 查询铁锂电池车辆
        List<Vehicle> liFePO4Vehicles = vehicleService.listByBatteryType("铁锂电池");
        
        // 验证
        assertNotNull(liFePO4Vehicles, "铁锂电池车辆列表不应为null");
        assertEquals(count2, liFePO4Vehicles.size(), "铁锂电池车辆数量应为" + count2);
        
        // 验证电池类型
        for (Vehicle vehicle : liFePO4Vehicles) {
            assertEquals("铁锂电池", vehicle.getBatteryType(), "车辆电池类型应为铁锂电池");
        }
    }
    
    /**
     * 测试根据IDs批量查询车辆
     */
    @Test
    @Transactional
    public void testListByIds() {
        // 创建多辆测试车辆
        List<Integer> carIds = new ArrayList<>();
        List<String> vids = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            String uniqueVid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            vids.add(uniqueVid);
            
            Vehicle vehicle = new Vehicle();
            vehicle.setVid(uniqueVid);
            vehicle.setBatteryType("批量查询测试");
            vehicleService.save(vehicle);
            carIds.add(vehicle.getCarId());
        }
        
        // 批量查询
        List<Vehicle> vehicles = vehicleService.listByIds(carIds);
        
        // 验证
        assertNotNull(vehicles, "批量查询结果不应为null");
        assertEquals(carIds.size(), vehicles.size(), "查询到的车辆数量应与ID数量一致");
        
        // 验证所有查询的车辆是否都在结果中
        List<Integer> foundIds = new ArrayList<>();
        List<String> foundVids = new ArrayList<>();
        
        for (Vehicle vehicle : vehicles) {
            foundIds.add(vehicle.getCarId());
            foundVids.add(vehicle.getVid());
        }
        
        assertTrue(foundIds.containsAll(carIds), "查询结果应包含所有指定的车辆ID");
        assertTrue(foundVids.containsAll(vids), "查询结果应包含所有指定的车辆VID");
    }
    
    /**
     * 测试更新车辆里程
     */
    @Test
    @Transactional
    public void testUpdateMileage() {
        // 更新里程
        BigDecimal newMileage = new BigDecimal("20000.00");
        boolean result = vehicleService.updateMileage(testCarId, newMileage);
        
        // 验证
        assertTrue(result, "更新里程应该成功");
        
        // 查询更新后的车辆
        Vehicle updated = vehicleService.getById(testCarId);
        assertNotNull(updated, "应能查询到更新后的车辆");
        assertEquals(0, newMileage.compareTo(updated.getMileage()), "里程应已更新");
    }
    
    /**
     * 测试更新电池健康度
     */
    @Test
    @Transactional
    public void testUpdateBatteryHealth() {
        // 更新电池健康度
        BigDecimal newHealth = new BigDecimal("0.87");
        boolean result = vehicleService.updateBatteryHealth(testCarId, newHealth);
        
        // 验证
        assertTrue(result, "更新电池健康度应该成功");
        
        // 查询更新后的车辆
        Vehicle updated = vehicleService.getById(testCarId);
        assertNotNull(updated, "应能查询到更新后的车辆");
        assertEquals(0, newHealth.compareTo(updated.getBatteryHealth()), "电池健康度应已更新");
    }
    
    /**
     * 测试缓存一致性
     */
    @Test
    @Transactional
    public void testCacheConsistency() {
        // 先获取一次车辆信息，触发缓存
        Vehicle vehicle1 = vehicleService.getById(testCarId);
        assertNotNull(vehicle1, "首次查询应能找到车辆");
        
        // 更新车辆信息
        vehicle1.setBatteryType("缓存一致性测试电池");
        vehicleService.updateById(vehicle1);
        
        // 再次查询，应获取到最新数据
        Vehicle vehicle2 = vehicleService.getById(testCarId);
        assertNotNull(vehicle2, "再次查询应能找到车辆");
        assertEquals("缓存一致性测试电池", vehicle2.getBatteryType(), "应获取到更新后的电池类型");
    }
} 