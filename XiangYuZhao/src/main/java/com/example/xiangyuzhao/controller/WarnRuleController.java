package com.example.xiangyuzhao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.dto.resp.BaseResponse;
import com.example.xiangyuzhao.entity.WarnRule;
import com.example.xiangyuzhao.service.WarnRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预警规则控制器 - 标准CRUD接口
 */
@Slf4j
@RestController
@RequestMapping("/api/warn_rule")
public class WarnRuleController {

    @Autowired
    private WarnRuleService warnRuleService;

    /**
     * 查询预警规则
     * @param params 查询条件
     * @param page 页码
     * @param size 每页大小
     * @return 查询结果
     */
    @GetMapping
    public BaseResponse<IPage<WarnRule>> query(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        
        QueryWrapper<WarnRule> queryWrapper = new QueryWrapper<>();
        
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
        IPage<WarnRule> result = warnRuleService.page(new Page<>(page, size), queryWrapper);
        return BaseResponse.success(result);
    }

    /**
     * 新增预警规则（支持单个或批量）
     * @param warnRules 预警规则
     * @return 操作结果
     */
    @PostMapping
    public BaseResponse<List<WarnRule>> insert(@RequestBody List<WarnRule> warnRules) {
        if (warnRules == null || warnRules.isEmpty()) {
            return BaseResponse.error(400, "没有要添加的预警规则数据");
        }

        if (warnRules.size() == 1) {
            warnRuleService.save(warnRules.get(0));
        } else {
            warnRuleService.saveBatch(warnRules);
        }

        return BaseResponse.success(warnRules);
    }

    /**
     * 更新预警规则（支持单个或批量）
     * @param warnRules 待更新的预警规则
     * @return 操作结果
     */
    @PutMapping
    public BaseResponse<Boolean> update(@RequestBody List<WarnRule> warnRules) {
        if (warnRules == null || warnRules.isEmpty()) {
            return BaseResponse.error(400, "没有要更新的预警规则数据");
        }

        if (warnRules.size() == 1) {
            return BaseResponse.success(warnRuleService.updateById(warnRules.get(0)));
        } else {
            return BaseResponse.success(warnRuleService.saveBatch(warnRules));
        }
    }

    /**
     * 删除预警规则（支持单个或批量）
     * @param ids 待删除的预警规则ID
     * @return 操作结果
     */
    @DeleteMapping
    public BaseResponse<Boolean> delete(@RequestParam("ids") String ids) {
        if (ids == null || ids.isEmpty()) {
            return BaseResponse.error(400, "没有要删除的预警规则ID");
        }

        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());

        if (idList.size() == 1) {
            return BaseResponse.success(warnRuleService.removeById(idList.get(0)));
        } else {
            return BaseResponse.success(warnRuleService.removeByIds(idList));
        }
    }
} 