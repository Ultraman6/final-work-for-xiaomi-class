package com.example.xiangyuzhao.dto.resp;

import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.entity.WarnInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 电池预警处理响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatteryWarnResp {
    
    /**
     * 信号ID
     */
    private Long signalId;
    
    /**
     * 车辆ID
     */
    private Integer carId;
    
    /**
     * 规则ID
     */
    private Integer warnId;
    
    /**
     * 处理时间
     */
    private Date processTime;
    
    /**
     * 解析后的信号数据
     */
    private Map<String, Object> parsedSignal;
    
    /**
     * 生成的预警列表
     */
    private List<WarnInfo> warnings;
    
    /**
     * 是否产生预警
     */
    private Boolean hasWarnings;
    
    /**
     * 最高预警级别
     */
    private Integer highestWarnLevel;
    
    /**
     * 构造函数
     * @param signal 电池信号
     * @param parsedSignal 解析后的信号数据
     * @param warnings 生成的预警列表
     */
    public BatteryWarnResp(BatterySignal signal, Map<String, Object> parsedSignal, List<WarnInfo> warnings) {
        this.signalId = signal.getId();
        this.carId = signal.getCarId();
        this.warnId = signal.getWarnId();
        this.processTime = new Date();
        this.parsedSignal = parsedSignal;
        this.warnings = warnings;
        this.hasWarnings = !warnings.isEmpty();
        
        // 计算最高预警级别（数值越小级别越高）
        this.highestWarnLevel = warnings.stream()
                .map(WarnInfo::getWarnLevel)
                .min(Integer::compareTo)
                .orElse(null);
    }
} 