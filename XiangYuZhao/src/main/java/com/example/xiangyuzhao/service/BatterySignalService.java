package com.example.xiangyuzhao.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.xiangyuzhao.entity.BatterySignal;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 电池信号服务接口
 */
public interface BatterySignalService extends IService<BatterySignal> {

    /**
     * 保存电池信号
     * @param signal 电池信号
     * @return 操作结果
     */
    boolean save(BatterySignal signal);

    /**
     * 批量保存电池信号
     * @param signals 电池信号列表
     * @return 操作结果
     */
    boolean saveBatch(List<BatterySignal> signals);

    /**
     * 根据ID获取电池信号
     * @param id 信号ID
     * @return 电池信号
     */
    BatterySignal getById(Long id);

    /**
     * 更新电池信号
     * @param signal 电池信号
     * @return 操作结果
     */
    boolean updateById(BatterySignal signal);

    /**
     * 标记电池信号为已处理
     * @param id 信号ID
     * @return 操作结果
     */
    boolean updateSignalProcessed(Long id);

    /**
     * 解析电池信号数据
     * @param signalData JSON格式的信号数据
     * @return 解析后的Map
     */
    Map<String, Object> parseSignalData(String signalData);

    /**
     * 获取车辆最新的电池信号
     * @param carId 车辆ID
     * @return 最新的电池信号
     */
    BatterySignal getLatestByCarId(Integer carId);

    /**
     * 分页查询车辆的电池信号
     * @param carId 车辆ID
     * @param current 当前页
     * @param size 每页大小
     * @return 分页结果
     */
    IPage<BatterySignal> pageByCarId(Integer carId, Integer current, Integer size);

    /**
     * 获取未处理的电池信号列表
     * @param limit 限制数量
     * @return 未处理的电池信号列表
     */
    List<BatterySignal> findUnprocessedSignals(Integer limit);
    
    /**
     * 获取最近时间内未处理的电池信号列表
     * @param minutes 分钟数
     * @param limit 限制数量
     * @return 未处理的电池信号列表
     */
    List<BatterySignal> findRecentUnprocessedSignals(Integer minutes, Integer limit);

    /**
     * 清理历史数据
     * @param carId 车辆ID
     * @param beforeDate 截止日期
     * @return 操作结果
     */
    boolean cleanHistoryData(Integer carId, Date beforeDate);
    
    /**
     * 批量删除电池信号
     * @param idList ID列表
     * @return 操作结果
     */
    boolean removeBatchByIds(List<Long> idList);
    
    /**
     * 根据时间范围查询未处理的信号（分区感知）
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param limit 限制数量
     * @return 未处理的电池信号列表
     */
    List<BatterySignal> findUnprocessedSignalsByTimeRange(Date startDate, Date endDate, Integer limit);
    
    /**
     * 通过时间分区清理历史数据
     * @param beforeDate 截止日期
     * @param batchSize 批次大小
     * @return 清理的记录数
     */
    int cleanupHistoricalDataByPartition(Date beforeDate, Integer batchSize);
    
    /**
     * 标记信号为处理中(processed=true, process_time=null)
     * @param signalId 信号ID
     * @return 操作结果
     */
    boolean markSignalProcessing(Long signalId);
    
    /**
     * 重置信号为未处理状态(processed=false, process_time=null)
     * @param signalId 信号ID
     * @return 操作结果
     */
    boolean resetSignalStatus(Long signalId);
    
    /**
     * 标记信号为已处理完成(processed=true, process_time=当前时间)
     * @param signalId 信号ID
     * @return 操作结果
     */
    boolean markSignalProcessed(Long signalId);
    
    /**
     * 查询处理中但超时的信号
     * @param timeoutMinutes 超时分钟数
     * @return 处理超时信号列表
     */
    List<BatterySignal> findStuckSignals(int timeoutMinutes);
} 