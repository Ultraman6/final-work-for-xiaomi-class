package com.example.xiangyuzhao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.dto.req.VehicleCreateReq;
import com.example.xiangyuzhao.dto.req.VehicleUpdateReq;
import com.example.xiangyuzhao.dto.resp.BaseResponse;
import com.example.xiangyuzhao.entity.Vehicle;
import com.example.xiangyuzhao.service.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 车辆控制器 - 标准CRUD接口
 */
@Slf4j
@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    /**
     * 查询车辆
     * @param params 查询条件
     * @param page 页码
     * @param size 每页大小
     * @return 查询结果
     */
    @GetMapping
    public BaseResponse<IPage<Vehicle>> query(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        
        QueryWrapper<Vehicle> queryWrapper = new QueryWrapper<>();
        
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
                        queryWrapper.between("create_time", value, params.get("endTime"));
                    } else if (!key.equals("endTime")) { // endTime已在startTime中处理
                        queryWrapper.eq(key, value);
                    }
                }
            });
        }
        
        // 默认按创建时间倒序
        queryWrapper.orderByDesc("create_time");
        
        // 执行分页查询
        IPage<Vehicle> result = vehicleService.page(page.intValue(), size.intValue(), queryWrapper);
        return BaseResponse.success(result);
    }

    /**
     * 新增车辆（支持单个或批量）
     * @param vehicles 车辆
     * @return 操作结果
     */
    @PostMapping
    public BaseResponse<List<Vehicle>> insert(@RequestBody List<Vehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return BaseResponse.error(400, "没有要添加的车辆数据");
        }

        if (vehicles.size() == 1) {
            vehicleService.save(vehicles.get(0));
        } else {
            vehicleService.saveBatch(vehicles);
        }

        return BaseResponse.success(vehicles);
    }

    /**
     * 更新车辆（支持单个或批量）
     * @param vehicles 待更新的车辆
     * @return 操作结果
     */
    @PutMapping
    public BaseResponse<Boolean> update(@RequestBody List<Vehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return BaseResponse.error(400, "没有要更新的车辆数据");
        }

        if (vehicles.size() == 1) {
            return BaseResponse.success(vehicleService.updateById(vehicles.get(0)));
        } else {
            return BaseResponse.success(vehicleService.saveBatch(vehicles));
        }
    }

    /**
     * 删除车辆（支持单个或批量）
     * @param ids 待删除的车辆ID
     * @return 操作结果
     */
    @DeleteMapping
    public BaseResponse<Boolean> delete(@RequestParam("ids") String ids) {
        if (ids == null || ids.isEmpty()) {
            return BaseResponse.error(400, "没有要删除的车辆ID");
        }

        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toList());

        if (idList.size() == 1) {
            return BaseResponse.success(vehicleService.removeById(idList.get(0)));
        } else {
            return BaseResponse.success(vehicleService.removeBatchByIds(idList));
        }
    }
} 