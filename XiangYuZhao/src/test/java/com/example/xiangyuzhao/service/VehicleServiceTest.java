package com.example.xiangyuzhao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.xiangyuzhao.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VehicleService单元测试
 */
@SpringBootTest
public class VehicleServiceTest {

    @Autowired
    private VehicleService vehicleService;
    
    /**
     * 测试前准备工作
     */
    @BeforeEach
    public void setUp() {
        // 不需要清理数据，因为每个测试方法都使用@Transactional，测试完成后会自动回滚
    }

    /**
     * 测试保存车辆信息
     */
    @Test
    @Transactional
    public void testSave() {
        // 创建车辆对象
        Vehicle vehicle = new Vehicle();
        vehicle.setCarId(null); // 自增ID
        vehicle.setBatteryType("三元电池");
        vehicle.setMileage(new BigDecimal("100.00"));
        vehicle.setBatteryHealth(new BigDecimal("99.50"));
        
        // 保存
        boolean result = vehicleService.save(vehicle);
        
        // 验证
        assertTrue(result, "保存车辆信息应该成功");
        assertNotNull(vehicle.getCarId(), "保存后应该生成车辆ID");
        assertNotNull(vehicle.getVid(), "保存后应该生成VID");
        assertEquals(16, vehicle.getVid().length(), "VID长度应该为16");
    }
    
    /**
     * 测试批量保存车辆信息
     */
    @Test
    @Transactional
    public void testSaveBatch() {
        // 创建多个车辆对象
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Vehicle vehicle = new Vehicle();
            vehicle.setBatteryType(i % 2 == 0 ? "三元电池" : "铁锂电池");
            vehicle.setMileage(new BigDecimal(1000 + i * 100));
            vehicle.setBatteryHealth(new BigDecimal(100 - i));
            vehicles.add(vehicle);
        }
        
        // 批量保存
        boolean result = vehicleService.saveBatch(vehicles);
        
        // 验证
        assertTrue(result, "批量保存车辆信息应该成功");
        vehicles.forEach(v -> {
            assertNotNull(v.getCarId(), "每个车辆对象都应该有ID");
            assertNotNull(v.getVid(), "每个车辆对象都应该有VID");
        });
    }
    
    /**
     * 测试查询车辆信息
     */
    @Test
    @Transactional
    public void testGetById() {
        // 先保存一条记录
        Vehicle vehicle = new Vehicle();
        vehicle.setBatteryType("铁锂电池");
        vehicle.setMileage(new BigDecimal("200.00"));
        vehicle.setBatteryHealth(new BigDecimal("95.00"));
        vehicleService.save(vehicle);
        
        Integer carId = vehicle.getCarId();
        
        // 查询
        Vehicle found = vehicleService.getById(carId);
        
        // 验证
        assertNotNull(found, "应该能查询到保存的车辆");
        assertEquals(carId, found.getCarId(), "车辆ID应该一致");
        assertEquals("铁锂电池", found.getBatteryType(), "电池类型应该一致");
        assertEquals(0, new BigDecimal("200.00").compareTo(found.getMileage()), "里程数应该一致");
        assertEquals(0, new BigDecimal("95.00").compareTo(found.getBatteryHealth()), "电池健康状态应该一致");
    }
    
    /**
     * 测试根据VID查询
     */
    @Test
    @Transactional
    public void testGetByVid() {
        // 先保存一条记录
        Vehicle vehicle = new Vehicle();
        vehicle.setBatteryType("三元电池");
        vehicle.setMileage(new BigDecimal("300.00"));
        vehicle.setBatteryHealth(new BigDecimal("98.00"));
        vehicleService.save(vehicle);
        
        String vid = vehicle.getVid();
        
        // 查询
        Vehicle found = vehicleService.getByVid(vid);
        
        // 验证
        assertNotNull(found, "应该能通过VID查询到车辆");
        assertEquals(vid, found.getVid(), "VID应该一致");
        assertEquals("三元电池", found.getBatteryType(), "电池类型应该一致");
    }
    
    /**
     * 测试更新车辆信息
     */
    @Test
    @Transactional
    public void testUpdateById() {
        // 先保存一条记录
        Vehicle vehicle = new Vehicle();
        vehicle.setBatteryType("铁锂电池");
        vehicle.setMileage(new BigDecimal("400.00"));
        vehicle.setBatteryHealth(new BigDecimal("100.00"));
        vehicleService.save(vehicle);
        
        Integer carId = vehicle.getCarId();
        
        // 更新里程和电池健康状态
        vehicle.setMileage(new BigDecimal("500.00"));
        vehicle.setBatteryHealth(new BigDecimal("98.50"));
        boolean result = vehicleService.updateById(vehicle);
        
        // 验证
        assertTrue(result, "更新车辆信息应该成功");
        Vehicle updated = vehicleService.getById(carId);
        assertEquals(0, new BigDecimal("500.00").compareTo(updated.getMileage()), "更新后里程应该一致");
        assertEquals(0, new BigDecimal("98.50").compareTo(updated.getBatteryHealth()), "更新后电池健康状态应该一致");
    }
    
    /**
     * 测试更新电池健康状态
     */
    @Test
    @Transactional
    public void testUpdateBatteryHealth() {
        // 先保存一条记录
        Vehicle vehicle = new Vehicle();
        vehicle.setBatteryType("三元电池");
        vehicle.setMileage(new BigDecimal("600.00"));
        vehicle.setBatteryHealth(new BigDecimal("95.00"));
        vehicleService.save(vehicle);
        
        Integer carId = vehicle.getCarId();
        
        // 更新电池健康状态
        boolean result = vehicleService.updateBatteryHealth(carId, new BigDecimal("93.50"));
        
        // 验证
        assertTrue(result, "更新电池健康状态应该成功");
        Vehicle updated = vehicleService.getById(carId);
        assertEquals(0, new BigDecimal("93.50").compareTo(updated.getBatteryHealth()), "更新后电池健康状态应该一致");
        assertEquals(0, new BigDecimal("600.00").compareTo(updated.getMileage()), "里程不应该变化");
    }
    
    /**
     * 测试更新里程
     */
    @Test
    @Transactional
    public void testUpdateMileage() {
        // 先保存一条记录
        Vehicle vehicle = new Vehicle();
        vehicle.setBatteryType("铁锂电池");
        vehicle.setMileage(new BigDecimal("800.00"));
        vehicle.setBatteryHealth(new BigDecimal("96.00"));
        vehicleService.save(vehicle);
        
        Integer carId = vehicle.getCarId();
        
        // 更新里程
        boolean result = vehicleService.updateMileage(carId, new BigDecimal("900.00"));
        
        // 验证
        assertTrue(result, "更新里程应该成功");
        Vehicle updated = vehicleService.getById(carId);
        assertEquals(0, new BigDecimal("900.00").compareTo(updated.getMileage()), "更新后里程应该一致");
        assertEquals(0, new BigDecimal("96.00").compareTo(updated.getBatteryHealth()), "电池健康状态不应该变化");
    }
    
    /**
     * 测试根据电池类型查询
     */
    @Test
    @Transactional
    public void testListByBatteryType() {
        // 先清理所有三元电池数据
        List<Vehicle> existingVehicles = vehicleService.listByBatteryType("三元电池");
        for (Vehicle v : existingVehicles) {
            vehicleService.removeById(v.getCarId());
        }
        
        // 再插入测试数据
        int count = 5; // 插入5条三元电池记录
        for (int i = 0; i < count; i++) {
            Vehicle vehicle = new Vehicle();
            vehicle.setBatteryType("三元电池");
            vehicle.setMileage(new BigDecimal(i * 100));
            vehicle.setBatteryHealth(new BigDecimal(100 - i));
            vehicleService.save(vehicle);
        }
        
        // 插入一些铁锂电池记录
        for (int i = 0; i < 3; i++) {
            Vehicle vehicle = new Vehicle();
            vehicle.setBatteryType("铁锂电池");
            vehicle.setMileage(new BigDecimal(i * 200));
            vehicle.setBatteryHealth(new BigDecimal(95 - i));
            vehicleService.save(vehicle);
        }
        
        // 查询三元电池的车辆
        List<Vehicle> list = vehicleService.listByBatteryType("三元电池");
        
        // 验证
        assertNotNull(list, "查询结果不应为null");
        assertEquals(count, list.size(), "应该查询到指定数量的三元电池记录");
        list.forEach(v -> {
            assertEquals("三元电池", v.getBatteryType(), "查询结果都应该是三元电池");
        });
    }
    
    /**
     * 测试分页查询
     */
    @Test
    @Transactional
    public void testPage() {
        // 插入足够多的测试数据以支持分页测试
        int totalRecords = 30; // 总共插入30条记录
        int pageSize = 8; // 每页8条记录
        
        for (int i = 0; i < totalRecords; i++) {
            Vehicle vehicle = new Vehicle();
            vehicle.setBatteryType(i % 2 == 0 ? "三元电池" : "铁锂电池");
            vehicle.setMileage(new BigDecimal(i * 100));
            vehicle.setBatteryHealth(new BigDecimal(100 - i * 0.5));
            vehicleService.save(vehicle);
        }
        
        // 分页查询第1页
        IPage<Vehicle> page1 = vehicleService.page(1, pageSize);
        
        // 验证第1页
        assertNotNull(page1, "第1页查询结果不应为null");
        assertEquals(1, page1.getCurrent(), "当前页应该是第1页");
        assertEquals(pageSize, page1.getSize(), "页大小应该是" + pageSize);
        assertEquals(pageSize, page1.getRecords().size(), "第1页应该有" + pageSize + "条记录");
        assertTrue(page1.getTotal() >= totalRecords, "总记录数应该大于等于插入的记录数");
        
        // 分页查询第2页
        IPage<Vehicle> page2 = vehicleService.page(2, pageSize);
        
        // 验证第2页
        assertNotNull(page2, "第2页查询结果不应为null");
        assertEquals(2, page2.getCurrent(), "当前页应该是第2页");
        assertEquals(pageSize, page2.getSize(), "页大小应该是" + pageSize);
        assertTrue(page2.getRecords().size() > 0, "第2页应该有记录");
    }
    
    /**
     * 测试删除
     */
    @Test
    @Transactional
    public void testRemoveById() {
        // 先保存一条记录
        Vehicle vehicle = new Vehicle();
        vehicle.setBatteryType("三元电池");
        vehicle.setMileage(new BigDecimal("1200.00"));
        vehicle.setBatteryHealth(new BigDecimal("92.50"));
        vehicleService.save(vehicle);
        
        Integer carId = vehicle.getCarId();
        
        // 删除
        boolean result = vehicleService.removeById(carId);
        
        // 验证
        assertTrue(result, "删除操作应该成功");
        assertNull(vehicleService.getById(carId), "删除后应该查询不到记录");
    }
    
    /**
     * 测试批量删除
     */
    @Test
    @Transactional
    public void testRemoveBatchByIds() {
        // 先保存多条记录
        List<Vehicle> vehicles = new ArrayList<>();
        List<Integer> carIds = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Vehicle vehicle = new Vehicle();
            vehicle.setBatteryType(i % 2 == 0 ? "三元电池" : "铁锂电池");
            vehicle.setMileage(new BigDecimal(2000 + i * 100));
            vehicle.setBatteryHealth(new BigDecimal(90 - i));
            vehicleService.save(vehicle);
            carIds.add(vehicle.getCarId());
            vehicles.add(vehicle);
        }
        
        // 批量删除
        boolean result = vehicleService.removeBatchByIds(carIds);
        
        // 验证
        assertTrue(result, "批量删除应该成功");
        for (Integer carId : carIds) {
            assertNull(vehicleService.getById(carId), "删除后应该查询不到记录");
        }
    }
    
    /**
     * 测试生成随机VID
     */
    @Test
    public void testGenerateRandomVid() {
        String vid = vehicleService.generateRandomVid();
        
        // 验证
        assertNotNull(vid, "生成的VID不应为null");
        assertEquals(16, vid.length(), "VID长度应该为16");
    }
    
    /**
     * 测试批量查询
     */
    @Test
    @Transactional
    public void testListByIds() {
        // 先保存多条记录
        List<Integer> carIds = new ArrayList<>();
        int count = 8;
        
        for (int i = 0; i < count; i++) {
            Vehicle vehicle = new Vehicle();
            vehicle.setBatteryType(i % 2 == 0 ? "三元电池" : "铁锂电池");
            vehicle.setMileage(new BigDecimal(3000 + i * 100));
            vehicle.setBatteryHealth(new BigDecimal(85 - i));
            vehicleService.save(vehicle);
            carIds.add(vehicle.getCarId());
        }
        
        // 批量查询
        List<Vehicle> vehicles = vehicleService.listByIds(carIds);
        
        // 验证
        assertNotNull(vehicles, "批量查询结果不应为null");
        assertEquals(count, vehicles.size(), "批量查询结果数量应该与插入数量一致");
        for (Vehicle vehicle : vehicles) {
            assertTrue(carIds.contains(vehicle.getCarId()), "批量查询结果应该包含所有指定ID的记录");
        }
    }
} 