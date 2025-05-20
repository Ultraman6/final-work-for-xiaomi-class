package com.example.xiangyuzhao.controller;

import com.example.xiangyuzhao.dto.req.BatterySignalUploadReq;
import com.example.xiangyuzhao.dto.resp.BaseResponse;
import com.example.xiangyuzhao.dto.resp.BatteryWarnResp;
import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.entity.Vehicle;
import com.example.xiangyuzhao.entity.WarnInfo;
import com.example.xiangyuzhao.entity.WarnRule;
import com.example.xiangyuzhao.service.BatterySignalService;
import com.example.xiangyuzhao.service.VehicleService;
import com.example.xiangyuzhao.service.WarnInfoService;
import com.example.xiangyuzhao.service.WarnRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 电池信号上报与预警控制器
 * 提供电池信号上报、预警处理和车辆预警查询功能
 */
@Slf4j
@RestController
public class ReportWarnController {

    @Autowired
    private BatterySignalService batterySignalService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private WarnInfoService warnInfoService;

    @Autowired
    private WarnRuleService warnRuleService;

    /**
     * 处理电池信号并返回预警信息（支持批量处理）
     * 
     * @param signalReqs 电池信号请求列表
     * @return 预警信息
     */
    @PostMapping("/api/warn")
    public BaseResponse<List<BatteryWarnResp>> processWarnings(@RequestBody @Validated List<BatterySignalUploadReq> signalReqs) {
        if (signalReqs == null || signalReqs.isEmpty()) {
            return BaseResponse.error(400, "没有要处理的信号数据");
        }

        List<BatteryWarnResp> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // 处理每个信号
        for (BatterySignalUploadReq req : signalReqs) {
            try {
                // 验证车辆存在
                Vehicle vehicle = vehicleService.getById(req.getCarId());
                if (vehicle == null) {
                    errors.add("车辆不存在: " + req.getCarId());
                    continue;
                }

                // 验证VID（如果提供）
                if (req.getVid() != null && !req.getVid().isEmpty() && !vehicle.getVid().equals(req.getVid())) {
                    errors.add("车辆VID验证失败: " + req.getCarId());
                    continue;
                }

                // 解析信号数据为Map，便于后续处理
                Object signalObj = req.getSignal();
                String signalJsonString;
                
                if (signalObj instanceof Map) {
                    signalJsonString = com.alibaba.fastjson.JSON.toJSONString(signalObj);
                } else {
                    signalJsonString = signalObj.toString();
                }
                
                Map<String, Object> parsedSignal = batterySignalService.parseSignalData(signalJsonString);
                
                // 根据warnId的情况分别处理
                if (req.getWarnId() == null) {
                    // 1. 获取该车辆电池类型的所有规则
                    List<WarnRule> rules = warnRuleService.listByBatteryType(vehicle.getBatteryType());
                    log.info("车辆ID: {}, 电池类型: {}, 找到{}个适用的规则", 
                             req.getCarId(), vehicle.getBatteryType(), rules.size());
                    
                    if (rules.isEmpty()) {
                        log.info("车辆ID: {}, 未找到适用的规则", req.getCarId());
                        continue; // 如果没有规则，则跳过处理
                    }
                    
                    // 创建可修改的信号数据副本
                    Map<String, Object> remainingSignalData = new HashMap<>(parsedSignal);
                    
                    // 用于收集所有预警信息
                    List<WarnInfo> allWarnings = new ArrayList<>();
                    // 用于收集创建的信号对象
                    List<BatterySignal> createdSignals = new ArrayList<>();
                    // 记录最后处理的信号对象（用于返回响应）
                    BatterySignal lastProcessedSignal = null;
                    // 跟踪已处理的规则ID，防止重复处理
                    Set<Integer> processedRuleIds = new HashSet<>();
                    
                    // 2. 依次应用每条规则
                    for (WarnRule rule : rules) {
                        try {
                            // 如果规则已经处理过，跳过
                            if (processedRuleIds.contains(rule.getWarnId())) {
                                log.info("规则ID: {} 已处理过，跳过", rule.getWarnId());
                                continue;
                            }
                            
                            // 解析规则
                            com.alibaba.fastjson.JSONObject ruleObj = (com.alibaba.fastjson.JSONObject) warnRuleService.parseRule(rule.getWarnId(), vehicle.getBatteryType());
                            if (ruleObj == null) {
                                log.warn("规则解析失败，规则ID: {}", rule.getWarnId());
                                continue;
                            }
                            
                            // 获取左右操作数
                            String leftOperand = ruleObj.getString("leftOperand");
                            String rightOperand = ruleObj.getString("rightOperand");
                            
                            // 检查信号中是否包含所需的操作数
                            if (!remainingSignalData.containsKey(leftOperand) || !remainingSignalData.containsKey(rightOperand)) {
                                log.info("信号中不包含规则所需的操作数，跳过规则ID: {}, leftOperand: {}, rightOperand: {}", 
                                        rule.getWarnId(), leftOperand, rightOperand);
                                continue;
                            }
                            
                            // 创建仅包含当前规则所需字段的信号数据
                            Map<String, Object> ruleSpecificSignalData = new HashMap<>();
                            ruleSpecificSignalData.put(leftOperand, remainingSignalData.get(leftOperand));
                            ruleSpecificSignalData.put(rightOperand, remainingSignalData.get(rightOperand));
                            
                            // 转换为JSON字符串
                            String ruleSpecificSignalJson = com.alibaba.fastjson.JSON.toJSONString(ruleSpecificSignalData);
                            log.info("规则ID: {}, 提取的信号数据: {}", rule.getWarnId(), ruleSpecificSignalJson);
                            
                            // 为匹配的规则创建信号对象
                            BatterySignal signal = new BatterySignal(
                                req.getCarId(),
                                rule.getWarnId(), // 使用规则的ID
                                ruleSpecificSignalJson, // 仅使用规则相关的字段
                                req.getSignalTime() != null ? req.getSignalTime() : new Date()
                            );
                            
                            // 保存到数据库，这样可以获取自动生成的ID
                            batterySignalService.save(signal);
                            
                            // 使用当前规则处理信号
                            List<WarnInfo> ruleWarnings = warnInfoService.processSignalWarn(signal.getCarId(), signal.getId());
                            
                            // 标记信号为已处理
                            batterySignalService.updateSignalProcessed(signal.getId());
                            
                            // 记录最后处理的信号
                            lastProcessedSignal = signal;
                            
                            // 标记规则为已处理
                            processedRuleIds.add(rule.getWarnId());
                            
                            if (ruleWarnings != null && !ruleWarnings.isEmpty()) {
                                allWarnings.addAll(ruleWarnings);
                                log.info("规则ID: {}, 名称: {}, 生成{}条预警", 
                                        rule.getWarnId(), rule.getWarnName(), ruleWarnings.size());
                                
                                // 从信号数据中移除已处理的操作数，避免重复处理
                                remainingSignalData.remove(leftOperand);
                                remainingSignalData.remove(rightOperand);
                                log.info("已移除操作数: {} 和 {}, 剩余操作数: {}", 
                                        leftOperand, rightOperand, remainingSignalData.keySet());
                            } else {
                                log.info("规则ID: {}, 名称: {}, 未触发预警", 
                                        rule.getWarnId(), rule.getWarnName());
                            }
                        } catch (Exception e) {
                            log.warn("处理规则ID {}时发生异常: {}", rule.getWarnId(), e.getMessage());
                        }
                    }
                    
                    // 如果至少处理了一条规则
                    if (lastProcessedSignal != null) {
                        // 构造一个代表性的响应信号（用于返回给客户端）
                        BatterySignal representativeSignal = new BatterySignal();
                        representativeSignal.setId(lastProcessedSignal.getId());
                        representativeSignal.setCarId(req.getCarId());
                        representativeSignal.setWarnId(null); // 设置为null表示这是多规则处理的结果
                        representativeSignal.setSignalData(signalJsonString); // 这里仍然保留完整的信号数据，便于客户端查看
                        representativeSignal.setSignalTime(lastProcessedSignal.getSignalTime());
                        representativeSignal.setProcessed(true);
                        representativeSignal.setProcessTime(new Date());
                        
                        // 添加到结果列表
                        results.add(new BatteryWarnResp(representativeSignal, parsedSignal, allWarnings));
                    }
                    
                } else {
                    // 对于指定了warnId的情况，按现有逻辑处理
                    BatterySignal signal = new BatterySignal(
                        req.getCarId(),
                        req.getWarnId(),
                        signalJsonString,
                        req.getSignalTime() != null ? req.getSignalTime() : new Date()
                    );
                    
                    // 保存信号
                    batterySignalService.save(signal);
                    
                    // 处理信号生成预警
                    List<WarnInfo> warnings = warnInfoService.processSignalWarn(signal.getCarId(), signal.getId());
                    
                    // 标记信号为已处理
                    batterySignalService.updateSignalProcessed(signal.getId());
                    
                    // 添加到结果列表
                    results.add(new BatteryWarnResp(signal, parsedSignal, warnings));
                }
            } catch (Exception e) {
                log.error("处理电池信号异常", e);
                errors.add("处理失败: " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            log.warn("部分信号处理出现错误: {}", errors);
        }

        return BaseResponse.success(results);
    }

    /**
     * 上报电池信号信息（支持批量处理）
     * 
     * @param signalReqs 电池信号请求列表
     * @return 保存结果
     */
    @PostMapping("/api/report")
    public BaseResponse<List<BatterySignal>> reportSignals(@RequestBody @Validated List<BatterySignalUploadReq> signalReqs) {
        if (signalReqs == null || signalReqs.isEmpty()) {
            return BaseResponse.error(400, "没有要上报的信号数据");
        }

        List<BatterySignal> savedSignals = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // 处理每个信号 （参考warn接口对warnid的设计）
        for (BatterySignalUploadReq req : signalReqs) {
            try {
                // 验证车辆存在
                Vehicle vehicle = vehicleService.getById(req.getCarId());
                if (vehicle == null) {
                    errors.add("车辆不存在: " + req.getCarId());
                    continue;
                }

                // 验证VID（如果提供）
                if (req.getVid() != null && !req.getVid().isEmpty() && !vehicle.getVid().equals(req.getVid())) {
                    errors.add("车辆VID验证失败: " + req.getCarId());
                    continue;
                }

                // 解析信号数据为字符串
                Object signalObj = req.getSignal();
                String signalJsonString;
                if (signalObj instanceof Map) {
                    signalJsonString = com.alibaba.fastjson.JSON.toJSONString(signalObj);
                } else {
                    signalJsonString = signalObj.toString();
                }
                
                // 根据warnId的情况分别处理
                if (req.getWarnId() == null) {
                    // 1. 获取该车辆电池类型的所有规则
                    List<WarnRule> rules = warnRuleService.listByBatteryType(vehicle.getBatteryType());
                    log.info("车辆ID: {}, 电池类型: {}, 找到{}个适用的规则", 
                             req.getCarId(), vehicle.getBatteryType(), rules.size());
                    
                    if (rules.isEmpty()) {
                        log.info("车辆ID: {}, 未找到适用的规则", req.getCarId());
                        continue; // 如果没有规则，则跳过处理
                    }
                    
                    // 获取解析后的信号数据
                    Map<String, Object> parsedSignal = batterySignalService.parseSignalData(signalJsonString);
                    // 创建可修改的信号数据副本
                    Map<String, Object> remainingSignalData = new HashMap<>(parsedSignal);
                    
                    // 用于收集创建的信号对象
                    List<BatterySignal> createdSignals = new ArrayList<>();
                    // 跟踪已处理的规则ID，防止重复处理
                    Set<Integer> processedRuleIds = new HashSet<>();
                    
                    // 2. 依次应用每条规则
                    for (WarnRule rule : rules) {
                        try {
                            // 如果规则已经处理过，跳过
                            if (processedRuleIds.contains(rule.getWarnId())) {
                                log.info("规则ID: {} 已处理过，跳过", rule.getWarnId());
                                continue;
                            }
                            
                            // 解析规则
                            com.alibaba.fastjson.JSONObject ruleObj = (com.alibaba.fastjson.JSONObject) warnRuleService.parseRule(rule.getWarnId(), vehicle.getBatteryType());
                            if (ruleObj == null) {
                                log.warn("规则解析失败，规则ID: {}", rule.getWarnId());
                                continue;
                            }
                            
                            // 获取左右操作数
                            String leftOperand = ruleObj.getString("leftOperand");
                            String rightOperand = ruleObj.getString("rightOperand");
                            
                            // 检查信号中是否包含所需的操作数
                            if (!remainingSignalData.containsKey(leftOperand) || !remainingSignalData.containsKey(rightOperand)) {
                                log.info("信号中不包含规则所需的操作数，跳过规则ID: {}, leftOperand: {}, rightOperand: {}", 
                                        rule.getWarnId(), leftOperand, rightOperand);
                                continue;
                            }
                            
                            // 为匹配的规则创建信号对象
                            BatterySignal signal = new BatterySignal(
                                req.getCarId(),
                                rule.getWarnId(), // 使用规则的ID
                                signalJsonString,
                                req.getSignalTime() != null ? req.getSignalTime() : new Date()
                            );
                            
                            // 保存信号
                            batterySignalService.save(signal);
                            
                            // 将创建的信号添加到结果列表
                            savedSignals.add(signal);
                            
                            // 标记规则为已处理
                            processedRuleIds.add(rule.getWarnId());
                            
                            // 从信号数据中移除已处理的操作数，避免重复处理
                            remainingSignalData.remove(leftOperand);
                            remainingSignalData.remove(rightOperand);
                            log.info("已移除操作数: {} 和 {}, 剩余操作数: {}", 
                                    leftOperand, rightOperand, remainingSignalData.keySet());
                        } catch (Exception e) {
                            log.warn("处理规则ID {}时发生异常: {}", rule.getWarnId(), e.getMessage());
                        }
                    }
                } else {
                    // 对于指定了warnId的情况，直接创建信号
                    BatterySignal signal = new BatterySignal(
                        req.getCarId(),
                        req.getWarnId(),  // 使用指定的warnId
                        signalJsonString,
                        req.getSignalTime() != null ? req.getSignalTime() : new Date()
                    );

                    // 保存信号
                    batterySignalService.save(signal);
                    savedSignals.add(signal);
                }
            } catch (Exception e) {
                log.error("保存电池信号异常", e);
                errors.add("保存失败: " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            log.warn("部分信号保存出现错误: {}", errors);
        }

        return BaseResponse.success(savedSignals);
    }

    /**
     * 查询预警信息
     * 
     * @param carId 车辆ID（必须）
     * @param warnId 预警ID（可选）
     * @param minWarnTime 最小预警时间（可选）
     * @param maxWarnTime 最大预警时间（可选）
     * @return 预警信息列表
     */
    @GetMapping("/api/search")
    public BaseResponse<List<WarnInfo>> searchWarnings(
            @RequestParam(name = "car_id") Integer carId,
            @RequestParam(name = "warn_id", required = false) Integer warnId,
            @RequestParam(name = "min_warn_time", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date minWarnTime,
            @RequestParam(name = "max_warn_time", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date maxWarnTime) {
        
        // 验证车辆存在
        Vehicle vehicle = vehicleService.getById(carId);
        if (vehicle == null) {
            return BaseResponse.error(404, "车辆不存在");
        }

        // 获取所有该车辆的预警
        List<WarnInfo> warnings = warnInfoService.listByCarId(carId);
        
        // 使用Stream API进行过滤
        return BaseResponse.success(warnings.stream()
                // 根据预警ID过滤
                .filter(warn -> warnId == null || warn.getWarnId().equals(warnId))
                                // 根据最小预警时间过滤
                .filter(warn -> minWarnTime == null || 
                       (warn.getWarnTime() != null && !warn.getWarnTime().before(minWarnTime)))
                // 根据最大预警时间过滤
                .filter(warn -> maxWarnTime == null || 
                       (warn.getWarnTime() != null && !warn.getWarnTime().after(maxWarnTime)))
                .collect(Collectors.toList()));
    }
} 