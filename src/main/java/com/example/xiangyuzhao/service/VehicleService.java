package com.example.xiangyuzhao.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.entity.Vehicle;

import java.math.BigDecimal;
import java.util.List;

/**
 * 车辆信息服务接口
 */
public interface VehicleService {

    /**
     * 新增车辆
     * @param vehicle 车辆信息
     * @return 是否成功
     */
    boolean save(Vehicle vehicle);

    /**
     * 批量新增车辆
     * @param vehicles 车辆信息列表
     * @return 是否成功
     */
    boolean saveBatch(List<Vehicle> vehicles);

    /**
     * 根据车架编号删除车辆
     * @param carId 车架编号
     * @return 是否成功
     */
    boolean removeById(Integer carId);

    /**
     * 批量删除车辆
     * @param carIds 车架编号列表
     * @return 是否成功
     */
    boolean removeBatchByIds(List<Integer> carIds);

    /**
     * 更新车辆信息
     * @param vehicle 车辆信息
     * @return 是否成功
     */
    boolean updateById(Vehicle vehicle);

    /**
     * 根据车架编号查询车辆
     * @param carId 车架编号
     * @return 车辆信息
     */
    Vehicle getById(Integer carId);

    /**
     * 根据VID查询车辆
     * @param vid 车辆识别码
     * @return 车辆信息
     */
    Vehicle getByVid(String vid);

    /**
     * 分页查询车辆
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    IPage<Vehicle> page(long page, long size);

    /**
     * 带条件的分页查询车辆
     * @param page 页码
     * @param size 每页大小
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    IPage<Vehicle> page(long page, long size, QueryWrapper<Vehicle> queryWrapper);

    /**
     * 根据电池类型查询车辆列表
     * @param batteryType 电池类型
     * @return 车辆列表
     */
    List<Vehicle> listByBatteryType(String batteryType);

    /**
     * 根据ID列表批量查询车辆
     * @param carIds 车架编号列表
     * @return 车辆列表
     */
    List<Vehicle> listByIds(List<Integer> carIds);

    /**
     * 更新车辆电池健康状态
     * @param carId 车架编号
     * @param batteryHealth 电池健康状态
     * @return 是否成功
     */
    boolean updateBatteryHealth(Integer carId, BigDecimal batteryHealth);

    /**
     * 更新车辆里程
     * @param carId 车架编号
     * @param mileage 总里程
     * @return 是否成功
     */
    boolean updateMileage(Integer carId, BigDecimal mileage);
    
    /**
     * 生成随机VID
     * @return 16位随机字符串
     */
    String generateRandomVid();
} 