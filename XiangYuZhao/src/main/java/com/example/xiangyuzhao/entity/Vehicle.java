package com.example.xiangyuzhao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("vehicle")
public class Vehicle {
    
    @TableId(value = "car_id", type = IdType.AUTO)
    private Integer carId;
    
    private String vid;
    
    private String batteryType;
    
    private BigDecimal mileage;
    
    private BigDecimal batteryHealth;
} 