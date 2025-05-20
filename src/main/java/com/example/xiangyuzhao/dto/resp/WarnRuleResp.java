package com.example.xiangyuzhao.dto.resp;

import lombok.Data;
import java.util.Date;

/**
 * 预警规则响应
 */
@Data
public class WarnRuleResp {
    /**
     * 主键ID
     */
    private Long id;
    
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
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
} 