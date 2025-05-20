package com.example.xiangyuzhao.dto.resp;

import lombok.Data;

import java.util.Date;

/**
 * 预警信息响应
 */
@Data
public class WarnInfoResp {
    /**
     * 车架编号
     */
    private Integer carId;
    
    /**
     * 电池类型
     */
    private String batteryType;
    
    /**
     * 预警规则名称
     */
    private String warnName;
    
    /**
     * 预警等级，0最高级别
     */
    private Integer warnLevel;
    
    /**
     * 预警完成时间
     */
    private Date warnTime;
    
    /**
     * 信号时间
     */
    private Date signalTime;
} 