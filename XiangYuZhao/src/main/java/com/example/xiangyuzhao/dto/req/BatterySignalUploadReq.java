package com.example.xiangyuzhao.dto.req;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.Map;

/**
 * 电池信号上传请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatterySignalUploadReq {
    /**
     * 车架编号
     */
    @NotNull(message = "车架编号不能为空")
    private Integer carId;
    
    /**
     * 预警编号
     * 1：电压差检测
     * 2：电流差检测
     * 如果为空，则对所有规则进行检查
     */
    private Integer warnId;
    
    /**
     * 电池信号数据，可以是JSON字符串或Map
     */
    @NotNull(message = "信号数据不能为空")
    private Object signal;
    
    /**
     * 车辆VID (可选)，用于双重验证
     */
    @Pattern(regexp = "[A-Z0-9]{16}", message = "VID格式不正确，应为16位大写字母和数字")
    private String vid;
    
    /**
     * 信号时间，如果为空则使用系统当前时间
     */
    private Date signalTime;
    
    /**
     * 将Map类型的信号数据转为JSON字符串
     * @return 处理后的BatterySignalUploadReq实例
     */
    public BatterySignalUploadReq convertMapToJson() {
        if (this.signal instanceof Map) {
            this.signal = com.alibaba.fastjson.JSON.toJSONString(this.signal);
        }
        return this;
    }
} 