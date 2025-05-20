package com.example.xiangyuzhao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 预警信息实体类
 */
@Data
@TableName("warn_info")
public class WarnInfo {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 车辆ID
     */
    private Integer carId;
    
    /**
     * 预警ID
     */
    private Integer warnId;
    
    /**
     * 预警名称
     */
    private String warnName;
    
    /**
     * 预警级别
     */
    private Integer warnLevel;
    
    /**
     * 信号ID
     */
    private Long signalId;
    
    /**
     * 信号时间
     */
    private Date signalTime;
    
    /**
     * 预警时间
     */
    private Date warnTime;
    
    /**
     * 默认构造函数，设置告警完成时间为当前时间
     */
    public WarnInfo() {
        this.warnTime = new Date();
    }
    
    /**
     * 完整构造函数
     * @param carId 车辆ID
     * @param warnId 告警规则ID
     * @param warnName 告警规则名称
     * @param warnLevel 告警级别
     * @param signalId 信号ID
     * @param signalTime 信号时间
     */
    public WarnInfo(Integer carId, Integer warnId, String warnName, Integer warnLevel, Long signalId, Date signalTime) {
        this.carId = carId;
        this.warnId = warnId;
        this.warnName = warnName;
        this.warnLevel = warnLevel;
        this.signalId = signalId;
        this.signalTime = signalTime;
        this.warnTime = new Date(); // 设置告警完成时间为当前时间
    }
    
    /**
     * 完整构造函数（包含告警完成时间）
     * @param carId 车辆ID
     * @param warnId 告警规则ID
     * @param warnName 告警规则名称
     * @param warnLevel 告警级别
     * @param signalId 信号ID
     * @param warnTime 告警完成时间
     * @param signalTime 信号时间
     */
    public WarnInfo(Integer carId, Integer warnId, String warnName, Integer warnLevel, Long signalId, Date warnTime, Date signalTime) {
        this.carId = carId;
        this.warnId = warnId;
        this.warnName = warnName;
        this.warnLevel = warnLevel;
        this.signalId = signalId;
        this.warnTime = warnTime != null ? warnTime : new Date(); // 如果提供的告警时间为null，则使用当前时间
        this.signalTime = signalTime;
    }
} 