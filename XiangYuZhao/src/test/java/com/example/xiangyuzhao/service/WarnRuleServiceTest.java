package com.example.xiangyuzhao.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.xiangyuzhao.entity.WarnRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WarnRuleService单元测试
 */
@SpringBootTest
public class WarnRuleServiceTest {

    @Autowired
    private WarnRuleService warnRuleService;
    
    /**
     * 测试前准备工作
     */
    @BeforeEach
    public void setUp() {
        // 获取所有规则并清理
        IPage<WarnRule> allRules = warnRuleService.page(1, 1000, null);
        if (allRules != null && allRules.getRecords() != null) {
            for (WarnRule rule : allRules.getRecords()) {
                warnRuleService.removeById(rule.getId());
            }
        }
        
        // 为确保清理干净，再次检查特定类型
        // 清理所有三元电池的规则
        List<WarnRule> rules1 = warnRuleService.listByBatteryType("三元电池");
        for (WarnRule rule : rules1) {
            warnRuleService.removeById(rule.getId());
        }
        
        // 清理所有铁锂电池的规则
        List<WarnRule> rules2 = warnRuleService.listByBatteryType("铁锂电池");
        for (WarnRule rule : rules2) {
            warnRuleService.removeById(rule.getId());
        }
        
        // 清理所有磷酸铁锂电池的规则
        List<WarnRule> rules3 = warnRuleService.listByBatteryType("磷酸铁锂电池");
        for (WarnRule rule : rules3) {
            warnRuleService.removeById(rule.getId());
        }
    }
    
    /**
     * 测试保存预警规则
     */
    @Test
    @Transactional
    public void testSave() {
        WarnRule warnRule = new WarnRule();
        warnRule.setWarnId(1);
        warnRule.setWarnName("电压差告警");
        warnRule.setBatteryType("三元电池");
        
        JSONObject ruleJson = new JSONObject();
        ruleJson.put("leftOperand", "Mx");
        ruleJson.put("rightOperand", "Mi");
        ruleJson.put("operator", 0); // 使用减法运算
        
        warnRule.setRule(ruleJson.toJSONString());
        
        boolean result = warnRuleService.save(warnRule);
        
        assertTrue(result, "保存预警规则应该成功");
        assertNotNull(warnRule.getId(), "ID不应为空");
    }

    /**
     * 测试批量保存预警规则
     */
    @Test
    @Transactional
    public void testSaveBatch() {
        List<WarnRule> warnRules = new ArrayList<>();
        
        WarnRule rule1 = new WarnRule();
        rule1.setWarnId(1);
        rule1.setWarnName("电压差告警");
        rule1.setBatteryType("三元电池");
        rule1.setRule(createVoltageRuleJson());
        warnRules.add(rule1);
        
        WarnRule rule2 = new WarnRule();
        rule2.setWarnId(2);
        rule2.setWarnName("电流差告警");
        rule2.setBatteryType("三元电池");
        rule2.setRule(createCurrentRuleJson());
        warnRules.add(rule2);
        
        WarnRule rule3 = new WarnRule();
        rule3.setWarnId(1);
        rule3.setWarnName("电压差告警");
        rule3.setBatteryType("铁锂电池");
        rule3.setRule(createVoltageRuleJson());
        warnRules.add(rule3);
        
        boolean result = warnRuleService.saveBatch(warnRules);
        
        assertTrue(result, "批量保存预警规则应该成功");
        
        // 验证三元电池规则
        List<WarnRule> rules = warnRuleService.listByBatteryType("三元电池");
        assertEquals(2, rules.size(), "三元电池规则数量应为2");
        
        // 验证铁锂电池规则
        List<WarnRule> liFePO4Rules = warnRuleService.listByBatteryType("铁锂电池");
        assertEquals(1, liFePO4Rules.size(), "铁锂电池规则数量应为1");
    }
    
    /**
     * 测试根据ID查询预警规则
     */
    @Test
    @Transactional
    public void testGetById() {
        // 先保存一条记录
        WarnRule warnRule = new WarnRule();
        warnRule.setWarnId(1);
        warnRule.setWarnName("电压差告警");
        warnRule.setBatteryType("三元电池");
        warnRule.setRule(createVoltageRuleJson());
        warnRuleService.save(warnRule);
        
        Long id = warnRule.getId();
        
        // 查询
        WarnRule found = warnRuleService.getById(id);
        
        // 验证
        assertNotNull(found, "根据ID查询应该能找到记录");
        assertEquals(id, found.getId(), "ID应该一致");
        assertEquals("三元电池", found.getBatteryType(), "电池类型应该一致");
        assertEquals("电压差告警", found.getWarnName(), "规则名称应该一致");
    }
    
    /**
     * 测试根据规则ID和电池类型查询规则
     */
    @Test
    @Transactional
    public void testGetByWarnIdAndBatteryType() {
        // 保存一条记录
        WarnRule warnRule = new WarnRule();
        warnRule.setWarnId(1);
        warnRule.setWarnName("电压差告警");
        warnRule.setBatteryType("三元电池");
        warnRule.setRule(createVoltageRuleJson());
        warnRuleService.save(warnRule);
        
        // 查询
        WarnRule found = warnRuleService.getByWarnIdAndBatteryType(1, "三元电池");
        
        // 验证
        assertNotNull(found, "根据预警ID和电池类型查询应该能找到记录");
        assertEquals(1, found.getWarnId(), "预警ID应该一致");
        assertEquals("三元电池", found.getBatteryType(), "电池类型应该一致");
        assertEquals("电压差告警", found.getWarnName(), "预警名称应该一致");
    }
    
    /**
     * 测试根据电池类型查询预警规则列表
     */
    @Test
    @Transactional
    public void testListByBatteryType() {
        // 先确保清空所有规则
        List<WarnRule> existingRules1 = warnRuleService.listByBatteryType("三元电池");
        for (WarnRule rule : existingRules1) {
            warnRuleService.removeById(rule.getId());
        }
        
        List<WarnRule> existingRules2 = warnRuleService.listByBatteryType("铁锂电池");
        for (WarnRule rule : existingRules2) {
            warnRuleService.removeById(rule.getId());
        }
        
        // 添加三元电池规则
        int count1 = 4;
        for (int i = 0; i < count1; i++) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(i + 1); // 不同的预警ID
            rule.setWarnName("电池告警" + (i + 1));
            rule.setBatteryType("三元电池");
            rule.setRule(createVoltageRuleJson());
            warnRuleService.save(rule);
        }
        
        // 添加铁锂电池规则
        int count2 = 3;
        for (int i = 0; i < count2; i++) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(i + 10); // 不同的预警ID，避免与三元电池规则冲突
            rule.setWarnName("铁锂告警" + (i + 1));
            rule.setBatteryType("铁锂电池");
            rule.setRule(createCurrentRuleJson());
            warnRuleService.save(rule);
        }
        
        // 查询三元电池规则
        List<WarnRule> ncmRules = warnRuleService.listByBatteryType("三元电池");
        
        // 验证
        assertNotNull(ncmRules, "三元电池规则列表不应为null");
        assertEquals(count1, ncmRules.size(), "三元电池规则数量应为" + count1);
        ncmRules.forEach(rule -> assertEquals("三元电池", rule.getBatteryType(), "电池类型应该是三元电池"));
        
        // 查询铁锂电池规则
        List<WarnRule> liFePO4Rules = warnRuleService.listByBatteryType("铁锂电池");
        
        // 验证
        assertNotNull(liFePO4Rules, "铁锂电池规则列表不应为null");
        assertEquals(count2, liFePO4Rules.size(), "铁锂电池规则数量应为" + count2);
        liFePO4Rules.forEach(rule -> assertEquals("铁锂电池", rule.getBatteryType(), "电池类型应该是铁锂电池"));
    }
    
    /**
     * 测试根据规则ID查询预警规则列表
     */
    @Test
    @Transactional
    public void testListByWarnId() {
        // 先确保清空所有规则
        List<WarnRule> existingRules1 = warnRuleService.listByBatteryType("三元电池");
        for (WarnRule rule : existingRules1) {
            warnRuleService.removeById(rule.getId());
        }
        
        List<WarnRule> existingRules2 = warnRuleService.listByBatteryType("铁锂电池");
        for (WarnRule rule : existingRules2) {
            warnRuleService.removeById(rule.getId());
        }
        
        // 创建预警ID为1的规则，应用于不同类型的电池
        int warnId = 1;
        
        WarnRule rule1 = new WarnRule();
        rule1.setWarnId(warnId);
        rule1.setWarnName("电压差告警");
        rule1.setBatteryType("三元电池");
        rule1.setRule(createVoltageRuleJson());
        warnRuleService.save(rule1);
        
        WarnRule rule2 = new WarnRule();
        rule2.setWarnId(warnId);
        rule2.setWarnName("电压差告警");
        rule2.setBatteryType("铁锂电池");
        rule2.setRule(createVoltageRuleJson());
        warnRuleService.save(rule2);
        
        WarnRule rule3 = new WarnRule();
        rule3.setWarnId(warnId);
        rule3.setWarnName("电压差告警");
        rule3.setBatteryType("磷酸铁锂电池");
        rule3.setRule(createVoltageRuleJson());
        warnRuleService.save(rule3);
        
        // 查询预警ID为1的规则
        List<WarnRule> rules = warnRuleService.listByWarnId(warnId);
        
        // 验证
        assertNotNull(rules, "规则列表不应为null");
        assertEquals(3, rules.size(), "预警ID为1的规则数量应为3");
        rules.forEach(rule -> assertEquals(warnId, rule.getWarnId(), "预警ID应该是" + warnId));
    }
    
    /**
     * 测试分页查询
     */
    @Test
    @Transactional
    public void testPage() {
        // 先确保清空所有规则
        List<WarnRule> existingRules1 = warnRuleService.listByBatteryType("三元电池");
        for (WarnRule rule : existingRules1) {
            warnRuleService.removeById(rule.getId());
        }
        
        List<WarnRule> existingRules2 = warnRuleService.listByBatteryType("铁锂电池");
        for (WarnRule rule : existingRules2) {
            warnRuleService.removeById(rule.getId());
        }
        
        // 添加10条规则
        int total = 10;
        for (int i = 0; i < total; i++) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(i + 1); // 不同的预警ID
            rule.setWarnName("测试规则" + (i + 1));
            rule.setBatteryType(i % 2 == 0 ? "三元电池" : "铁锂电池");
            rule.setRule(i % 2 == 0 ? createVoltageRuleJson() : createCurrentRuleJson());
            warnRuleService.save(rule);
        }
        
        // 获取实际的总记录数
        IPage<WarnRule> countPage = warnRuleService.page(1, 1, null);
        int actualTotalRecords = (int)countPage.getTotal();
        
        // 第一页，每页4条
        int pageSize = 4;
        int current = 1;
        IPage<WarnRule> page1 = warnRuleService.page(current, pageSize, null);
        
        // 验证
        assertNotNull(page1, "分页结果不应为null");
        assertEquals(current, page1.getCurrent(), "当前页应为" + current);
        assertEquals(pageSize, page1.getSize(), "每页大小应为" + pageSize);
        assertEquals(total, page1.getTotal(), "总记录数应为" + total);
        assertEquals((int)Math.ceil((double)total / pageSize), page1.getPages(), "总页数计算有误");
        assertEquals(pageSize, page1.getRecords().size(), "第一页记录数应为" + pageSize);
        
        // 第二页，每页4条
        current = 2;
        IPage<WarnRule> page2 = warnRuleService.page(current, pageSize, null);
        
        // 验证
        assertNotNull(page2, "分页结果不应为null");
        assertEquals(current, page2.getCurrent(), "当前页应为" + current);
        assertEquals(pageSize, page2.getSize(), "每页大小应为" + pageSize);
        assertEquals(pageSize, page2.getRecords().size(), "第二页记录数应为" + pageSize);
        
        // 第三页，每页4条，只有2条记录
        current = 3;
        IPage<WarnRule> page3 = warnRuleService.page(current, pageSize, null);
        
        // 验证
        assertNotNull(page3, "分页结果不应为null");
        assertEquals(current, page3.getCurrent(), "当前页应为" + current);
        assertEquals(pageSize, page3.getSize(), "每页大小应为" + pageSize);
        assertEquals(total - (pageSize * 2), page3.getRecords().size(), "第三页记录数应为" + (total - (pageSize * 2)));
    }
    
    /**
     * 测试根据ID删除预警规则
     */
    @Test
    @Transactional
    public void testRemoveById() {
        // 先保存一条记录
        WarnRule warnRule = new WarnRule();
        warnRule.setWarnId(1);
        warnRule.setWarnName("电压差告警");
        warnRule.setBatteryType("三元电池");
        warnRule.setRule(createVoltageRuleJson());
        warnRuleService.save(warnRule);
        
        Long id = warnRule.getId();
        
        // 删除
        boolean result = warnRuleService.removeById(id);
        
        // 验证
        assertTrue(result, "删除操作应该成功");
        WarnRule found = warnRuleService.getById(id);
        assertNull(found, "删除后应该查询不到记录");
    }
    
    /**
     * 测试根据ID列表批量删除预警规则
     */
    @Test
    @Transactional
    public void testRemoveByIds() {
        // 先保存多条记录
        List<Long> ids = new ArrayList<>();
        
        // 三元电池规则
        for (int i = 0; i < 3; i++) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(i + 1); // 不同的规则ID
            rule.setWarnName("电池告警" + (i + 1));
            rule.setBatteryType("三元电池");
            rule.setRule(createVoltageRuleJson());
            warnRuleService.save(rule);
            ids.add(rule.getId());
        }
        
        // 检查保存是否成功
        for (Long id : ids) {
            assertNotNull(warnRuleService.getById(id), "保存后应该能查到记录");
        }
        
        // 批量删除
        boolean result = warnRuleService.removeByIds(ids);
        
        // 验证
        assertTrue(result, "批量删除操作应该成功");
        
        // 检查是否都删除了
        for (Long id : ids) {
            assertNull(warnRuleService.getById(id), "删除后应该查询不到记录");
        }
    }
    
    /**
     * 测试更新预警规则
     */
    @Test
    @Transactional
    public void testUpdateById() {
        // 先保存一条记录
        WarnRule warnRule = new WarnRule();
        warnRule.setWarnId(1);
        warnRule.setWarnName("电压差告警");
        warnRule.setBatteryType("三元电池");
        warnRule.setRule(createVoltageRuleJson());
        warnRuleService.save(warnRule);
        
        Long id = warnRule.getId();
        
        // 修改
        warnRule.setWarnName("电压差告警-修改后");
        JSONObject newRule = new JSONObject();
        newRule.put("leftOperand", "Vx");
        newRule.put("rightOperand", "Vi");
        newRule.put("operator", 1); // 使用加法运算
        warnRule.setRule(newRule.toJSONString());
        
        boolean result = warnRuleService.updateById(warnRule);
        
        // 验证
        assertTrue(result, "更新操作应该成功");
        WarnRule found = warnRuleService.getById(id);
        assertNotNull(found, "更新后应该能查询到记录");
        assertEquals("电压差告警-修改后", found.getWarnName(), "规则名称应该已更新");
        assertTrue(found.getRule().contains("Vx"), "规则内容应该已更新");
    }
    
    /**
     * 生成电压差告警规则的JSON
     */
    private String createVoltageRuleJson() {
        JSONObject ruleJson = new JSONObject();
        ruleJson.put("leftOperand", "Mx");
        ruleJson.put("rightOperand", "Mi");
        ruleJson.put("operator", 0); // 使用减法运算
        
        List<JSONObject> rules = new ArrayList<>();
        
        JSONObject rule1 = new JSONObject();
        rule1.put("minValue", 5.0);
        rule1.put("maxValue", null);
        rule1.put("includeMin", true);
        rule1.put("includeMax", false);
        rule1.put("level", 0);
        rules.add(rule1);
        
        JSONObject rule2 = new JSONObject();
        rule2.put("minValue", 3.0);
        rule2.put("maxValue", 5.0);
        rule2.put("includeMin", true);
        rule2.put("includeMax", false);
        rule2.put("level", 1);
        rules.add(rule2);
        
        ruleJson.put("rules", rules);
        
        return ruleJson.toJSONString();
    }
    
    /**
     * 生成电流差告警规则的JSON
     */
    private String createCurrentRuleJson() {
        JSONObject ruleJson = new JSONObject();
        ruleJson.put("leftOperand", "Ix");
        ruleJson.put("rightOperand", "Ii");
        ruleJson.put("operator", 0); // 使用减法运算
        
        List<JSONObject> rules = new ArrayList<>();
        
        JSONObject rule1 = new JSONObject();
        rule1.put("minValue", 3.0);
        rule1.put("maxValue", null);
        rule1.put("includeMin", true);
        rule1.put("includeMax", false);
        rule1.put("level", 0);
        rules.add(rule1);
        
        JSONObject rule2 = new JSONObject();
        rule2.put("minValue", 1.0);
        rule2.put("maxValue", 3.0);
        rule2.put("includeMin", true);
        rule2.put("includeMax", false);
        rule2.put("level", 1);
        rules.add(rule2);
        
        ruleJson.put("rules", rules);
        
        return ruleJson.toJSONString();
    }
} 