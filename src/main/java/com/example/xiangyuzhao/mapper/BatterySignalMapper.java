package com.example.xiangyuzhao.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.xiangyuzhao.entity.BatterySignal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface BatterySignalMapper extends BaseMapper<BatterySignal> {
    /**
     * Find all signals for a specific vehicle
     * @param carId Car ID
     * @return List of battery signals
     */
    List<BatterySignal> findByCarId(@Param("carId") Integer carId);
    
    /**
     * Find the most recent signal for a vehicle
     * @param carId Car ID
     * @return Latest battery signal
     */
    BatterySignal findLatestByCarId(@Param("carId") Integer carId);
    
    /**
     * Find signals within a time range
     * @param carId Car ID
     * @param startTime Start time
     * @param endTime End time
     * @return List of battery signals
     */
    List<BatterySignal> findByCarIdAndTimeRange(
        @Param("carId") Integer carId,
        @Param("startTime") Date startTime,
        @Param("endTime") Date endTime
    );
    
    /**
     * Save multiple signals at once (batch)
     * @param signals List of battery signals
     * @return Number of rows affected
     */
    int saveSignalBatch(@Param("list") List<BatterySignal> signals);
    
    /**
     * Get signals by car ID with pagination
     * @param page Pagination information
     * @param carId Car ID
     * @return Paginated signals
     */
    IPage<BatterySignal> findByCarIdWithPagination(
        IPage<BatterySignal> page,
        @Param("carId") Integer carId
    );
    
    /**
     * Delete signals before a certain date for a car
     * @param carId Car ID
     * @param beforeDate Before date
     * @return Number of rows affected
     */
    int deleteSignalsBeforeDate(
        @Param("carId") Integer carId,
        @Param("beforeDate") Date beforeDate
    );
    
    /**
     * 查询未处理的信号列表（按时间顺序）
     * 使用processed索引优化查询
     * @param limit 最大返回条数
     * @return 未处理的信号列表
     */
    @Select("SELECT * FROM battery_signal WHERE processed = 0 ORDER BY signal_time ASC LIMIT #{limit}")
    List<BatterySignal> findUnprocessedSignals(@Param("limit") int limit);
    
    /**
     * 查询指定时间范围内未处理的信号列表
     * 使用processed索引和signal_time索引优化查询
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 最大返回条数
     * @return 未处理的信号列表
     */
    @Select("SELECT * FROM battery_signal WHERE processed = 0 AND signal_time BETWEEN #{startTime} AND #{endTime} ORDER BY signal_time ASC LIMIT #{limit}")
    List<BatterySignal> findUnprocessedSignalsByTimeRange(
        @Param("startTime") Date startTime, 
        @Param("endTime") Date endTime, 
        @Param("limit") int limit
    );
    
    /**
     * 查询最近时间段未处理的信号列表
     * 适用于定时任务的高效扫描
     * @param minutesAgo 多少分钟前
     * @param limit 最大返回条数
     * @return 未处理的信号列表
     */
    @Select("SELECT * FROM battery_signal WHERE processed = 0 AND signal_time >= DATE_SUB(NOW(), INTERVAL #{minutesAgo} MINUTE) ORDER BY signal_time ASC LIMIT #{limit}")
    List<BatterySignal> findRecentUnprocessedSignals(
        @Param("minutesAgo") int minutesAgo,
        @Param("limit") int limit
    );
    
    /**
     * 更新信号为已处理状态
     * @param id 信号ID
     * @param processTime 处理时间
     * @return 影响的行数
     */
    @Update("UPDATE battery_signal SET processed = 1, process_time = #{processTime} WHERE id = #{id}")
    int updateSignalProcessed(@Param("id") Long id, @Param("processTime") Date processTime);
    
    /**
     * 批量更新信号为已处理状态
     * @param ids 信号ID列表
     * @param processTime 处理时间
     * @return 影响的行数
     */
    int updateBatchSignalProcessed(@Param("ids") List<Long> ids, @Param("processTime") Date processTime);
    
    /**
     * 标记信号为处理中状态(processed=true, process_time=null)
     * @param id 信号ID
     * @return 影响的行数
     */
    @Update("UPDATE battery_signal SET processed = 1, process_time = null WHERE id = #{id} AND processed = 0")
    int markSignalProcessing(@Param("id") Long id);
    
    /**
     * 重置信号为未处理状态(processed=false, process_time=null)
     * @param id 信号ID
     * @return 影响的行数
     */
    @Update("UPDATE battery_signal SET processed = 0, process_time = null WHERE id = #{id}")
    int resetSignalStatus(@Param("id") Long id);
    
    /**
     * 标记信号为处理完成状态(processed=true, process_time=当前时间)
     * @param id 信号ID
     * @return 影响的行数
     */
    @Update("UPDATE battery_signal SET processed = 1, process_time = now() WHERE id = #{id} AND processed = 1 AND process_time IS NULL")
    int markSignalProcessed(@Param("id") Long id);
    
    /**
     * 查询卡在处理中状态的信号(processed=true, process_time=null, 超过指定时间)
     * @param minutes 超时分钟数
     * @return 处理中但可能卡住的信号列表
     */
    @Select("SELECT * FROM battery_signal WHERE processed = 1 AND process_time IS NULL AND signal_time < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    List<BatterySignal> findStuckSignals(@Param("minutes") int minutes);
} 