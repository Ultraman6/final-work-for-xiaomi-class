package com.example.xiangyuzhao.dto.req;

import lombok.Data;

/**
 * 预警规则创建请求
 */
@Data
public class WarnRuleCreateReq {
    /**
     * 预警编号
     */
    private Integer warnId;
    
    /**
     * 预警名称
     */
    private String warnName;
    
    /**
     * 电池类型
     */
    private String batteryType;
    
    /**
     * 规则JSON字符串
     */
    private String rule;
} 