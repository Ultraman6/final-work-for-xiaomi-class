package com.example.xiangyuzhao.dto.req;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;

/**
 * 车辆更新请求
 */
@Data
public class VehicleUpdateReq {
    /**
     * 车架编号 - 必须提供以识别要更新的车辆
     */
    @NotNull(message = "车架编号不能为空")
    private Integer carId;
    
    /**
     * 电池类型
     */
    @Pattern(regexp = "三元电池|铁锂电池", message = "电池类型必须是'三元电池'或'铁锂电池'")
    private String batteryType;
    
    /**
     * 总里程(km)
     */
    @DecimalMin(value = "0.0", message = "里程不能为负数")
    @Digits(integer = 10, fraction = 2, message = "里程格式不正确，整数部分最多10位，小数部分最多2位")
    private BigDecimal mileage;
    
    /**
     * 电池健康状态(%)
     */
    @DecimalMin(value = "0.0", message = "电池健康状态不能为负数")
    @Digits(integer = 3, fraction = 2, message = "电池健康状态格式不正确，整数部分最多3位，小数部分最多2位")
    private BigDecimal batteryHealth;
} 