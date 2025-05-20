package com.example.xiangyuzhao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("warn_rule")
public class WarnRule {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Integer warnId;
    
    private String warnName;
    
    private String batteryType;
    
    private String rule;
} 