package com.example.xiangyuzhao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.xiangyuzhao.entity.WarnRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarnRuleMapper extends BaseMapper<WarnRule> {
    /**
     * Find rules by warn ID and battery type
     * @param warnId Warn ID
     * @param batteryType Battery type
     * @return WarnRule entity
     */
    WarnRule findByWarnIdAndBatteryType(@Param("warnId") Integer warnId, @Param("batteryType") String batteryType);
    
    /**
     * Find all rules for a specific battery type
     * @param batteryType Battery type
     * @return List of warning rules
     */
    List<WarnRule> findAllRulesByBatteryType(@Param("batteryType") String batteryType);
    
    /**
     * Find all rules by warn ID
     * @param warnId Warn ID
     * @return List of warning rules for different battery types
     */
    List<WarnRule> findByWarnId(@Param("warnId") Integer warnId);
    
    /**
     * Update rule JSON definition
     * @param id Primary key
     * @param rule Rule JSON string
     * @return Number of rows affected
     */
    int updateRule(@Param("id") Long id, @Param("rule") String rule);
    
    /**
     * Find rule by name and battery type
     * @param warnName Rule name
     * @param batteryType Battery type
     * @return WarnRule entity
     */
    WarnRule findByWarnNameAndBatteryType(@Param("warnName") String warnName, @Param("batteryType") String batteryType);
} 