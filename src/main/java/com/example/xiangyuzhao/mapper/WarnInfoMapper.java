package com.example.xiangyuzhao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.xiangyuzhao.entity.WarnInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface WarnInfoMapper extends BaseMapper<WarnInfo> {
    /**
     * Find all warnings for a specific vehicle
     * @param carId Car ID
     * @return List of warning information
     */
    List<WarnInfo> findByCarId(@Param("carId") Integer carId);
    
    /**
     * Find warnings for a vehicle filtered by level
     * @param carId Car ID
     * @param warnLevel Warning level
     * @return List of warning information
     */
    List<WarnInfo> findByCarIdAndWarnLevel(
        @Param("carId") Integer carId,
        @Param("warnLevel") Integer warnLevel
    );
    
    /**
     * Find warnings triggered by a specific warn rule
     * @param warnId Warn ID
     * @return List of warning information
     */
    List<WarnInfo> findByWarnId(@Param("warnId") Integer warnId);
    
    /**
     * Count the number of warnings for a vehicle
     * @param carId Car ID
     * @return Count of warnings
     */
    int countWarningsByCarId(@Param("carId") Integer carId);
    
    /**
     * Find warnings by signal ID
     * @param signalId Signal ID
     * @return List of warning information
     */
    List<WarnInfo> findBySignalId(@Param("signalId") Long signalId);
    
    /**
     * Find warnings with pagination
     * @param page Pagination information
     * @param carId Car ID (optional)
     * @param warnLevel Warning level (optional)
     * @return Paginated warning information
     */
    IPage<WarnInfo> findWarningsWithPagination(
        IPage<WarnInfo> page,
        @Param("carId") Integer carId,
        @Param("warnLevel") Integer warnLevel
    );
    
    /**
     * Get statistics of warnings by level
     * @param startDate Start date
     * @param endDate End date
     * @return Map of warning level to count
     */
    List<Map<String, Object>> getWarningStatsByLevel(
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );
    
    /**
     * Batch insert warn info records
     * @param warnInfoList List of warning information
     * @return Number of rows affected
     */
    int batchInsert(@Param("list") List<WarnInfo> warnInfoList);
} 