package com.example.xiangyuzhao.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.xiangyuzhao.entity.WarnInfo;
import org.springframework.cache.annotation.Cacheable;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 预警信息服务接口
 */
public interface WarnInfoService extends IService<WarnInfo> {

    /**
     * 新增预警信息
     * @param warnInfo 预警信息
     * @return 是否成功
     */
    boolean save(WarnInfo warnInfo);

    /**
     * 批量保存预警信息
     * @param warnInfoList 预警信息列表
     * @return 是否成功
     */
    boolean saveBatch(List<WarnInfo> warnInfoList);

    /**
     * 根据ID删除预警信息
     * @param id 预警信息ID
     * @return 是否成功
     */
    boolean removeById(Long id);

    /**
     * 批量删除预警信息
     * @param ids 预警信息ID列表
     * @return 是否成功
     */
    boolean removeByIds(List<Long> ids);

    /**
     * 更新预警信息
     * @param warnInfo 预警信息
     * @return 是否成功
     */
    boolean updateById(WarnInfo warnInfo);

    /**
     * 根据ID获取预警信息
     * @param id 预警信息ID
     * @return 预警信息
     */
    WarnInfo getById(Long id);

    /**
     * 根据车辆ID查询预警信息列表
     * @param carId 车辆ID
     * @return 预警信息列表
     */
    List<WarnInfo> listByCarId(Integer carId);

    /**
     * 根据车辆ID和预警级别查询预警信息
     * @param carId 车辆ID
     * @param warnLevel 预警级别
     * @return 预警信息列表
     */
    List<WarnInfo> listByCarIdAndWarnLevel(Integer carId, Integer warnLevel);

    /**
     * 根据预警ID查询预警信息
     * @param warnId 预警ID
     * @return 预警信息列表
     */
    List<WarnInfo> listByWarnId(Integer warnId);

    /**
     * 分析电池信号并生成预警信息
     * @param carId 车辆ID
     * @param signalId 信号ID
     * @param batteryType 电池类型
     * @return 生成的预警信息列表
     */
    List<WarnInfo> analyzeSignalAndGenerateWarn(Integer carId, Long signalId, String batteryType);

    /**
     * 根据信号ID查询预警信息
     * @param signalId 信号ID
     * @return 预警信息列表
     */
    List<WarnInfo> listBySignalId(Long signalId);

    @Cacheable(value = "warnInfo", key = "'count_' + #carId")
    int countByCarId(Integer carId);

    /**
     * 分页查询预警信息
     * @param current 当前页
     * @param size 每页大小
     * @param carId 车辆ID (可为null)
     * @param warnLevel 预警级别 (可为null)
     * @return 分页结果
     */
    IPage<WarnInfo> page(int current, int size, Integer carId, Integer warnLevel);

    /**
     * 分页查询预警信息（支持条件构造器）
     * @param page 分页参数
     * @param queryWrapper 条件构造器
     * @return 分页结果
     */
    IPage<WarnInfo> page(Page<WarnInfo> page, QueryWrapper<WarnInfo> queryWrapper);

    /**
     * 获取车辆预警统计
     * @param carId 车辆ID
     * @return 统计结果
     */
    Map<String, Object> getWarnStatsByCarId(Integer carId);

    /**
     * 获取预警统计信息
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 各等级预警统计
     */
    List<Map<String, Object>> getWarnStats(Date startDate, Date endDate);

    /**
     * 处理信号触发预警
     * @param carId 车辆ID
     * @param signalId 信号ID
     * @return 生成的预警列表
     */
    List<WarnInfo> processSignalWarn(Integer carId, Long signalId);

    /**
     * 获取车辆未处理的预警信息
     * @param carId 车辆ID
     * @return 未处理的预警列表
     */
    List<WarnInfo> findUnhandledWarningsByCarId(Integer carId);

    /**
     * 设置预警为已处理
     * @param id 预警ID
     * @return 是否处理成功
     */
    boolean setWarnHandled(Long id);
} 