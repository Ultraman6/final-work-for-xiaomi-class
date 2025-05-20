package com.example.xiangyuzhao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("battery_signal")
public class BatterySignal {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Integer carId;
    
    private Integer warnId;
    
    private String signalData;
    
    private Date signalTime;
    
    private Boolean processed;
    
    private Date processTime;
    
    /**
     * 默认构造函数，设置信号时间为当前时间，处理状态为未处理
     */
    public BatterySignal() {
        this.signalTime = new Date();
        this.processed = false;
    }
    
    /**
     * 构造函数
     * @param carId 车辆ID
     * @param warnId 告警规则ID
     * @param signalData 信号数据
     */
    public BatterySignal(Integer carId, Integer warnId, String signalData) {
        this.carId = carId;
        this.warnId = warnId;
        this.signalData = signalData;
        this.signalTime = new Date(); // 设置信号时间为当前时间
        this.processed = false; // 设置处理状态为未处理
    }
    
    /**
     * 构造函数（包含信号时间）
     * @param carId 车辆ID
     * @param warnId 告警规则ID
     * @param signalData 信号数据
     * @param signalTime 信号时间
     */
    public BatterySignal(Integer carId, Integer warnId, String signalData, Date signalTime) {
        this.carId = carId;
        this.warnId = warnId;
        this.signalData = signalData;
        this.signalTime = signalTime != null ? signalTime : new Date(); // 如果信号时间为null，则使用当前时间
        this.processed = false; // 设置处理状态为未处理
    }
    
    /**
     * 标记信号为已处理
     */
    public void markAsProcessed() {
        this.processed = true;
        this.processTime = new Date();
    }
} 