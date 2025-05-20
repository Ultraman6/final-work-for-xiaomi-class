package com.example.xiangyuzhao.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 电池信号消息传输对象
 * 用于Kafka消息队列中传递电池信号信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatterySignalMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 电池信号ID
     */
    private Long signalId;
    
    /**
     * 车辆ID
     */
    private Integer carId;
    
    /**
     * 告警ID
     */
    private Integer warnId;
} 