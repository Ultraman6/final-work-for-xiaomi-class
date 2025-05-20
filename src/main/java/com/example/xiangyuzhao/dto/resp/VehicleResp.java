package com.example.xiangyuzhao.dto.resp;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 车辆信息响应
 */
@Data
public class VehicleResp {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 车辆识别码
     */
    private String vid;
    
    /**
     * 车架编号
     */
    private Integer carId;
    
    /**
     * 电池类型
     */
    private String batteryType;
    
    /**
     * 总里程(km)
     */
    private BigDecimal mileage;
    
    /**
     * 电池健康状态(%)
     */
    private BigDecimal batteryHealth;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
} 