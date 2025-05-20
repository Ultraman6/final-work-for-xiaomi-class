package com.example.xiangyuzhao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.dto.resp.BaseResponse;
import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.service.BatterySignalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 电池信号控制器 - 标准CRUD接口
 */
@Slf4j
@RestController
@RequestMapping("/api/battery_signal")
public class BatterySignalController {

    @Autowired
    private BatterySignalService batterySignalService;

    /**
     * 查询电池信号
     * @param params 查询条件
     * @param page 页码
     * @param size 每页大小
     * @return 查询结果
     */
    @GetMapping
    public BaseResponse<IPage<BatterySignal>> query(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        
        QueryWrapper<BatterySignal> queryWrapper = new QueryWrapper<>();
        
        // 动态构建查询条件
        if (params != null) {
            params.forEach((key, value) -> {
                if (value != null) {
                    // 处理特殊查询条件
                    if (key.endsWith("_gt")) {
                        queryWrapper.gt(key.substring(0, key.length() - 3), value);
                    } else if (key.endsWith("_lt")) {
                        queryWrapper.lt(key.substring(0, key.length() - 3), value);
                    } else if (key.endsWith("_like")) {
                        queryWrapper.like(key.substring(0, key.length() - 5), value);
                    } else if (key.equals("startTime") && params.containsKey("endTime")) {
                        queryWrapper.between("signal_time", value, params.get("endTime"));
                    } else if (!key.equals("endTime")) { // endTime已在startTime中处理
                        queryWrapper.eq(key, value);
                    }
                }
            });
        }
        
        // 默认按创建时间倒序
        queryWrapper.orderByDesc("signal_time");
        
        // 执行分页查询
        IPage<BatterySignal> result = batterySignalService.page(new Page<>(page, size), queryWrapper);
        return BaseResponse.success(result);
    }

    /**
     * 新增电池信号（支持单个或批量）
     * @param signals 电池信号
     * @return 操作结果
     */
    @PostMapping
    public BaseResponse<List<BatterySignal>> insert(@RequestBody List<BatterySignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return BaseResponse.error(400, "没有要添加的电池信号数据");
        }

        if (signals.size() == 1) {
            batterySignalService.save(signals.get(0));
        } else {
            batterySignalService.saveBatch(signals);
        }

        return BaseResponse.success(signals);
    }

    /**
     * 更新电池信号（支持单个或批量）
     * @param signals 待更新的电池信号
     * @return 操作结果
     */
    @PutMapping
    public BaseResponse<Boolean> update(@RequestBody List<BatterySignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return BaseResponse.error(400, "没有要更新的电池信号数据");
        }

        if (signals.size() == 1) {
            return BaseResponse.success(batterySignalService.updateById(signals.get(0)));
        } else {
            return BaseResponse.success(batterySignalService.saveBatch(signals));
        }
    }

    /**
     * 删除电池信号（支持单个或批量）
     * @param ids 待删除的电池信号ID
     * @return 操作结果
     */
    @DeleteMapping
    public BaseResponse<Boolean> delete(@RequestParam("ids") String ids) {
        if (ids == null || ids.isEmpty()) {
            return BaseResponse.error(400, "没有要删除的电池信号ID");
        }

        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());

        if (idList.size() == 1) {
            return BaseResponse.success(batterySignalService.removeById(idList.get(0)));
        } else {
            return BaseResponse.success(batterySignalService.removeBatchByIds(idList));
        }
    }
} 