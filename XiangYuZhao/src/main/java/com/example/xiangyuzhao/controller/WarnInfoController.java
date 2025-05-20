package com.example.xiangyuzhao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.dto.resp.BaseResponse;
import com.example.xiangyuzhao.entity.WarnInfo;
import com.example.xiangyuzhao.service.WarnInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预警信息控制器 - 标准CRUD接口
 */
@Slf4j
@RestController
@RequestMapping("/api/warn_info")
public class WarnInfoController {

    @Autowired
    private WarnInfoService warnInfoService;

    /**
     * 查询预警信息
     * @param params 查询条件
     * @param page 页码
     * @param size 每页大小
     * @return 查询结果
     */
    @GetMapping
    public BaseResponse<IPage<WarnInfo>> query(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        
        QueryWrapper<WarnInfo> queryWrapper = new QueryWrapper<>();
        
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
                        queryWrapper.between("warn_time", value, params.get("endTime"));
                    } else if (!key.equals("endTime")) { // endTime已在startTime中处理
                        queryWrapper.eq(key, value);
                    }
                }
            });
        }
        
        // 默认按预警时间倒序，最新的预警在前面
        queryWrapper.orderByDesc("warn_time");
        
        // 执行分页查询
        IPage<WarnInfo> result = warnInfoService.page(new Page<>(page, size), queryWrapper);
        return BaseResponse.success(result);
    }

    /**
     * 新增预警信息（支持单个或批量）
     * @param warnInfos 预警信息
     * @return 操作结果
     */
    @PostMapping
    public BaseResponse<List<WarnInfo>> insert(@RequestBody List<WarnInfo> warnInfos) {
        if (warnInfos == null || warnInfos.isEmpty()) {
            return BaseResponse.error(400, "没有要添加的预警信息数据");
        }

        // 设置创建时间
        Date now = new Date();
        for (WarnInfo warnInfo : warnInfos) {
            if (warnInfo.getWarnTime() == null) {
                warnInfo.setWarnTime(now);
            }
        }

        if (warnInfos.size() == 1) {
            warnInfoService.save(warnInfos.get(0));
        } else {
            warnInfoService.saveBatch(warnInfos);
        }

        return BaseResponse.success(warnInfos);
    }

    /**
     * 更新预警信息（支持单个或批量）
     * @param warnInfos 待更新的预警信息
     * @return 操作结果
     */
    @PutMapping
    public BaseResponse<Boolean> update(@RequestBody List<WarnInfo> warnInfos) {
        if (warnInfos == null || warnInfos.isEmpty()) {
            return BaseResponse.error(400, "没有要更新的预警信息数据");
        }

        if (warnInfos.size() == 1) {
            return BaseResponse.success(warnInfoService.updateById(warnInfos.get(0)));
        } else {
            return BaseResponse.success(warnInfoService.saveBatch(warnInfos));
        }
    }

    /**
     * 删除预警信息（支持单个或批量）
     * @param ids 待删除的预警信息ID
     * @return 操作结果
     */
    @DeleteMapping
    public BaseResponse<Boolean> delete(@RequestParam("ids") String ids) {
        if (ids == null || ids.isEmpty()) {
            return BaseResponse.error(400, "没有要删除的预警信息ID");
        }

        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());

        if (idList.size() == 1) {
            return BaseResponse.success(warnInfoService.removeById(idList.get(0)));
        } else {
            return BaseResponse.success(warnInfoService.removeByIds(idList));
        }
    }
} 