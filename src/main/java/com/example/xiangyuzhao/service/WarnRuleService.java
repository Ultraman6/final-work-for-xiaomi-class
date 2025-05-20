package com.example.xiangyuzhao.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.entity.WarnRule;
import java.util.List;

/**
 * 预警规则服务接口
 */
public interface WarnRuleService {

    /**
     * 新增预警规则
     * @param warnRule 预警规则
     * @return 是否成功
     */
    boolean save(WarnRule warnRule);

    /**
     * 批量新增预警规则
     * @param warnRules 预警规则列表
     * @return 是否成功
     */
    boolean saveBatch(List<WarnRule> warnRules);

    /**
     * 根据ID删除预警规则
     * @param id 规则ID
     * @return 是否成功
     */
    boolean removeById(Long id);

    /**
     * 批量删除预警规则
     * @param ids 规则ID列表
     * @return 是否成功
     */
    boolean removeByIds(List<Long> ids);

    /**
     * 更新预警规则
     * @param warnRule 预警规则
     * @return 是否成功
     */
    boolean updateById(WarnRule warnRule);

    /**
     * 根据ID查询预警规则
     * @param id 规则ID
     * @return 预警规则
     */
    WarnRule getById(Long id);

    /**
     * 查询所有预警规则
     * @return 预警规则列表
     */
    List<WarnRule> list();

    /**
     * 根据预警编号和电池类型查询预警规则
     * @param warnId 预警编号
     * @param batteryType 电池类型
     * @return 预警规则
     */
    WarnRule getByWarnIdAndBatteryType(Integer warnId, String batteryType);

    /**
     * 根据电池类型查询所有预警规则
     * @param batteryType 电池类型
     * @return 预警规则列表
     */
    List<WarnRule> listByBatteryType(String batteryType);

    /**
     * 根据预警编号查询所有电池类型的预警规则
     * @param warnId 预警编号
     * @return 预警规则列表
     */
    List<WarnRule> listByWarnId(Integer warnId);

    /**
     * 更新预警规则的JSON定义
     * @param id 规则ID
     * @param rule 规则JSON定义
     * @return 是否成功
     */
    boolean updateRule(Long id, String rule);

    /**
     * 获取规则JSON定义并解析
     * @param warnId 预警编号
     * @param batteryType 电池类型
     * @return 解析后的规则对象
     */
    Object parseRule(Integer warnId, String batteryType);
    
    /**
     * 分页查询预警规则
     * @param current 当前页
     * @param size 每页大小
     * @param batteryType 电池类型（可为null）
     * @return 分页结果
     */
    IPage<WarnRule> page(int current, int size, String batteryType);
    
    /**
     * 分页查询预警规则（支持条件构造器）
     * @param page 分页参数
     * @param queryWrapper 条件构造器
     * @return 分页结果
     */
    IPage<WarnRule> page(Page<WarnRule> page, QueryWrapper<WarnRule> queryWrapper);
} 