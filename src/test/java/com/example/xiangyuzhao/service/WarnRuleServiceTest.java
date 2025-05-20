package com.example.xiangyuzhao.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WarnRuleService单元测试
 * 全面覆盖所有方法和核心功能
 */
@SpringBootTest
public class WarnRuleServiceTest {

    @Autowired
    private WarnRuleService warnRuleService;
    
    @Autowired
    private CacheService cacheService;
    
    private Random random = new Random();
    private int testWarnId;
    private Long testRuleId;
    
    /**
     * 测试前准备工作
     */
    @BeforeEach
    public void setUp() {
        // 生成随机告警ID，避免冲突
        testWarnId = random.nextInt(90000) + 10000;
        
        // 创建测试用告警规则
        WarnRule warnRule = new WarnRule();
        warnRule.setWarnId(testWarnId);
        warnRule.setWarnName("测试电压差告警");
        warnRule.setBatteryType("三元电池");
        warnRule.setRule(createVoltageRuleJson());
        
        // 保存并记录ID
        warnRuleService.save(warnRule);
        testRuleId = warnRule.getId();
    }
    
    /**
     * 测试保存预警规则
     */
    @Test
    @Transactional
    public void testSave() {
        WarnRule warnRule = new WarnRule();
        warnRule.setWarnId(testWarnId + 100); // 使用不同的ID
        warnRule.setWarnName("电压差告警");
        warnRule.setBatteryType("三元电池");
        
        JSONObject ruleJson = new JSONObject();
        ruleJson.put("leftOperand", "Mx");
        ruleJson.put("rightOperand", "Mi");
        ruleJson.put("operator", 1); // 使用减法运算
        
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
        
        // 使用随机ID避免冲突
        int baseId = testWarnId + 200;
        
        WarnRule rule1 = new WarnRule();
        rule1.setWarnId(baseId);
        rule1.setWarnName("电压差告警");
        rule1.setBatteryType("三元电池");
        rule1.setRule(createVoltageRuleJson());
        warnRules.add(rule1);
        
        WarnRule rule2 = new WarnRule();
        rule2.setWarnId(baseId + 1);
        rule2.setWarnName("电流差告警");
        rule2.setBatteryType("三元电池");
        rule2.setRule(createCurrentRuleJson());
        warnRules.add(rule2);
        
        WarnRule rule3 = new WarnRule();
        rule3.setWarnId(baseId + 2);
        rule3.setWarnName("电压差告警");
        rule3.setBatteryType("铁锂电池");
        rule3.setRule(createVoltageRuleJson());
        warnRules.add(rule3);
        
        boolean result = warnRuleService.saveBatch(warnRules);
        
        assertTrue(result, "批量保存预警规则应该成功");
        
        // 验证所有规则都有ID
        for (WarnRule rule : warnRules) {
            assertNotNull(rule.getId(), "每个规则的ID都不应为空");
        }
        
        // 验证三元电池规则
        List<WarnRule> rules = warnRuleService.listByBatteryType("三元电池");
        assertTrue(rules.size() >= 3, "三元电池规则数量应至少为3");
        
        // 验证铁锂电池规则
        List<WarnRule> liFePO4Rules = warnRuleService.listByBatteryType("铁锂电池");
        assertTrue(liFePO4Rules.size() >= 1, "铁锂电池规则数量应至少为1");
    }
    
    /**
     * 测试根据ID查询预警规则
     */
    @Test
    @Transactional
    public void testGetById() {
        // 使用setUp中创建的测试规则
        WarnRule found = warnRuleService.getById(testRuleId);
        
        // 验证
        assertNotNull(found, "根据ID查询应该能找到记录");
        assertEquals(testRuleId, found.getId(), "ID应该一致");
        assertEquals(testWarnId, found.getWarnId(), "预警规则ID应该一致");
        assertEquals("三元电池", found.getBatteryType(), "电池类型应该一致");
        assertEquals("测试电压差告警", found.getWarnName(), "规则名称应该一致");
    }
    
    /**
     * 测试查询不存在的ID
     */
    @Test
    @Transactional
    public void testGetByNonExistentId() {
        WarnRule found = warnRuleService.getById(999999L);
        assertNull(found, "查询不存在的ID应返回null");
    }
    
    /**
     * 测试根据规则ID和电池类型查询规则
     */
    @Test
    @Transactional
    public void testGetByWarnIdAndBatteryType() {
        // 使用setUp中创建的测试规则
        WarnRule found = warnRuleService.getByWarnIdAndBatteryType(testWarnId, "三元电池");
        
        // 验证
        assertNotNull(found, "根据预警ID和电池类型查询应该能找到记录");
        assertEquals(testWarnId, found.getWarnId(), "预警ID应该一致");
        assertEquals("三元电池", found.getBatteryType(), "电池类型应该一致");
        assertEquals("测试电压差告警", found.getWarnName(), "预警名称应该一致");
    }
    
    /**
     * 测试查询不存在的预警ID和电池类型组合
     */
    @Test
    @Transactional
    public void testGetByNonExistentWarnIdAndBatteryType() {
        WarnRule found = warnRuleService.getByWarnIdAndBatteryType(99999, "不存在的电池类型");
        assertNull(found, "查询不存在的组合应返回null");
    }
    
    /**
     * 测试获取所有规则
     */
    @Test
    @Transactional
    public void testList() {
        // 添加多个规则
        for (int i = 0; i < 5; i++) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(testWarnId + 300 + i);
            rule.setWarnName("测试规则" + i);
            rule.setBatteryType(i % 2 == 0 ? "三元电池" : "铁锂电池");
            rule.setRule(i % 2 == 0 ? createVoltageRuleJson() : createCurrentRuleJson());
            warnRuleService.save(rule);
        }
        
        // 查询所有规则
        List<WarnRule> allRules = warnRuleService.list();
        
        // 验证
        assertNotNull(allRules, "规则列表不应为null");
        assertTrue(allRules.size() >= 6, "规则数量应至少为6"); // 5个新增的 + 1个setUp中的
    }
    
    /**
     * 测试根据电池类型查询预警规则列表
     */
    @Test
    @Transactional
    public void testListByBatteryType() {
        // 添加三元电池规则
        int count1 = 4;
        int baseId1 = testWarnId + 400; // 避免冲突
        
        for (int i = 0; i < count1; i++) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(baseId1 + i); // 不同的预警ID
            rule.setWarnName("电池告警" + (i + 1));
            rule.setBatteryType("三元电池");
            rule.setRule(createVoltageRuleJson());
            warnRuleService.save(rule);
        }
        
        // 添加铁锂电池规则
        int count2 = 3;
        int baseId2 = baseId1 + count1 + 1; // 确保ID不冲突
        
        for (int i = 0; i < count2; i++) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(baseId2 + i); // 不同的预警ID，避免与三元电池规则冲突
            rule.setWarnName("铁锂告警" + (i + 1));
            rule.setBatteryType("铁锂电池");
            rule.setRule(createCurrentRuleJson());
            warnRuleService.save(rule);
        }
        
        // 查询三元电池规则
        List<WarnRule> ncmRules = warnRuleService.listByBatteryType("三元电池");
        
        // 验证
        assertNotNull(ncmRules, "三元电池规则列表不应为null");
        assertTrue(ncmRules.size() >= count1 + 1, "三元电池规则数量应至少为" + (count1 + 1)); // +1是因为setUp中创建的
        
        // 验证电池类型
        for (WarnRule rule : ncmRules) {
            assertEquals("三元电池", rule.getBatteryType(), "电池类型应该是三元电池");
        }
        
        // 查询铁锂电池规则
        List<WarnRule> liFePO4Rules = warnRuleService.listByBatteryType("铁锂电池");
        
        // 验证
        assertNotNull(liFePO4Rules, "铁锂电池规则列表不应为null");
        assertTrue(liFePO4Rules.size() >= count2, "铁锂电池规则数量应至少为" + count2);
        
        // 验证电池类型
        for (WarnRule rule : liFePO4Rules) {
            assertEquals("铁锂电池", rule.getBatteryType(), "电池类型应该是铁锂电池");
        }
    }
    
    /**
     * 测试根据规则ID查询预警规则列表
     */
    @Test
    @Transactional
    public void testListByWarnId() {
        // 创建一个固定规则ID
        int warnId = testWarnId + 500; // 避免冲突
        
        // 为不同电池类型创建相同规则ID的规则
        List<String> batteryTypes = Arrays.asList("三元电池", "铁锂电池", "磷酸铁锂电池");
        for (String batteryType : batteryTypes) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(warnId);
            rule.setWarnName(batteryType + "电压差告警");
            rule.setBatteryType(batteryType);
            rule.setRule(createVoltageRuleJson());
            warnRuleService.save(rule);
        }
        
        // 查询指定规则ID的规则
        List<WarnRule> rules = warnRuleService.listByWarnId(warnId);
        
        // 验证
        assertNotNull(rules, "规则列表不应为null");
        assertEquals(batteryTypes.size(), rules.size(), "规则数量应该等于电池类型数量");
        
        // 验证规则ID
        for (WarnRule rule : rules) {
            assertEquals(warnId, rule.getWarnId(), "预警ID应该一致");
        }
        
        // 验证电池类型
        List<String> foundTypes = rules.stream()
            .map(WarnRule::getBatteryType)
            .collect(Collectors.toList());
        
        for (String type : batteryTypes) {
            assertTrue(foundTypes.contains(type), "应该包含 " + type + " 电池类型的规则");
        }
    }
    
    /**
     * 测试解析规则
     */
    @Test
    @Transactional
    public void testParseRule() {
        // 解析设置的规则
        Object parsedRule = warnRuleService.parseRule(testWarnId, "三元电池");
        
        // 验证
        assertNotNull(parsedRule, "解析结果不应为null");
        assertTrue(parsedRule instanceof JSONObject, "解析结果应为JSONObject");
        
        JSONObject ruleObj = (JSONObject) parsedRule;
        assertTrue(ruleObj.containsKey("leftOperand"), "解析结果应包含leftOperand字段");
        assertEquals("Mx", ruleObj.getString("leftOperand"), "leftOperand值应正确");
        assertEquals("Mi", ruleObj.getString("rightOperand"), "rightOperand值应正确");
    }
    
    /**
     * 测试解析不存在的规则
     */
    @Test
    @Transactional
    public void testParseNonExistentRule() {
        Object parsedRule = warnRuleService.parseRule(99999, "不存在的电池类型");
        assertNull(parsedRule, "解析不存在的规则应返回null");
    }
    
    /**
     * 测试分页查询预警规则
     */
    @Test
    @Transactional
    public void testPage() {
        // 创建足够多的测试数据
        int totalCount = 15; // 总记录数
        int baseId = testWarnId + 600; // 避免冲突
        String[] batteryTypes = {"三元电池", "铁锂电池", "磷酸铁锂电池"};
        
        for (int i = 0; i < totalCount; i++) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(baseId + i);
            rule.setWarnName("测试规则" + (i + 1));
            rule.setBatteryType(batteryTypes[i % batteryTypes.length]);
            rule.setRule(i % 2 == 0 ? createVoltageRuleJson() : createCurrentRuleJson());
            warnRuleService.save(rule);
        }
        
        // 测试不带条件的分页查询
        IPage<WarnRule> page1 = warnRuleService.page(1, 5, null);
        
        // 验证
        assertNotNull(page1, "分页结果不应为null");
        assertEquals(1, page1.getCurrent(), "当前页应为1");
        assertEquals(5, page1.getSize(), "每页大小应为5");
        assertEquals(5, page1.getRecords().size(), "第一页应有5条记录");
        assertTrue(page1.getTotal() >= totalCount + 1, "总记录数应至少为" + (totalCount + 1)); // +1是因为setUp中创建的
        
        // 测试带条件的分页查询 - 按电池类型筛选
        IPage<WarnRule> page2 = warnRuleService.page(1, 5, "三元电池");
        
        // 验证
        assertNotNull(page2, "条件分页结果不应为null");
        assertEquals(1, page2.getCurrent(), "当前页应为1");
        assertEquals(5, page2.getSize(), "每页大小应为5");
        assertTrue(page2.getRecords().size() > 0, "应有记录");
        
        // 验证筛选结果
        for (WarnRule rule : page2.getRecords()) {
            assertEquals("三元电池", rule.getBatteryType(), "筛选后电池类型应为三元电池");
        }
    }
    
    /**
     * 测试使用自定义Page和QueryWrapper的分页查询
     */
    @Test
    @Transactional
    public void testPageWithWrapper() {
        // 创建足够多的测试数据
        int totalCount = 15; // 总记录数
        int baseId = testWarnId + 700; // 避免冲突
        
        for (int i = 0; i < totalCount; i++) {
            WarnRule rule = new WarnRule();
            rule.setWarnId(baseId + i);
            rule.setWarnName("分页测试规则" + (i + 1));
            rule.setBatteryType(i % 2 == 0 ? "三元电池" : "铁锂电池");
            rule.setRule(i % 2 == 0 ? createVoltageRuleJson() : createCurrentRuleJson());
            warnRuleService.save(rule);
        }
        
        // 创建Page对象
        Page<WarnRule> pageParam = new Page<>(1, 10);
        
        // 创建查询条件
        QueryWrapper<WarnRule> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("warn_name", "分页测试");
        
        // 执行分页查询
        IPage<WarnRule> pageResult = warnRuleService.page(pageParam, queryWrapper);
        
        // 验证
        assertNotNull(pageResult, "分页结果不应为null");
        assertEquals(1, pageResult.getCurrent(), "当前页应为1");
        assertEquals(10, pageResult.getSize(), "每页大小应为10");
        assertTrue(pageResult.getRecords().size() > 0, "应有记录");
        assertTrue(pageResult.getTotal() >= totalCount, "总记录数应至少为" + totalCount);
        
        // 验证筛选结果
        for (WarnRule rule : pageResult.getRecords()) {
            assertTrue(rule.getWarnName().contains("分页测试"), "查询结果名称应包含'分页测试'");
        }
    }
    
    /**
     * 创建电压差规则JSON字符串
     */
    private String createVoltageRuleJson() {
        return "{\"leftOperand\":\"Mx\",\"rightOperand\":\"Mi\",\"operator\":1,\"rules\":[" +
                "{\"minValue\":5.0,\"maxValue\":null,\"includeMin\":true,\"includeMax\":false,\"level\":0}," +
                "{\"minValue\":3.0,\"maxValue\":5.0,\"includeMin\":true,\"includeMax\":false,\"level\":1}," +
                "{\"minValue\":1.0,\"maxValue\":3.0,\"includeMin\":true,\"includeMax\":false,\"level\":2}," +
                "{\"minValue\":0.6,\"maxValue\":1.0,\"includeMin\":true,\"includeMax\":false,\"level\":3}," +
                "{\"minValue\":0.2,\"maxValue\":0.6,\"includeMin\":true,\"includeMax\":false,\"level\":4}" +
                "]}";
    }
    
    /**
     * 创建电流差规则JSON字符串
     */
    private String createCurrentRuleJson() {
        return "{\"leftOperand\":\"Ix\",\"rightOperand\":\"Ii\",\"operator\":1,\"rules\":[" +
                "{\"minValue\":3.0,\"maxValue\":null,\"includeMin\":true,\"includeMax\":false,\"level\":0}," +
                "{\"minValue\":1.0,\"maxValue\":3.0,\"includeMin\":true,\"includeMax\":false,\"level\":1}," +
                "{\"minValue\":0.2,\"maxValue\":1.0,\"includeMin\":true,\"includeMax\":false,\"level\":2}" +
                "]}";
    }
} 