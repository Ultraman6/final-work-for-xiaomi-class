package com.example.xiangyuzhao.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.entity.Vehicle;
import com.example.xiangyuzhao.entity.WarnRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BatterySignalService单元测试
 * 全面覆盖所有方法和核心功能
 */
@SpringBootTest
public class BatterySignalServiceTest {

    @Autowired
    private BatterySignalService batterySignalService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private WarnRuleService warnRuleService;

    @Autowired
    private CacheService cacheService;

    private Integer testCarId;
    private Integer testWarnId;
    private Long testSignalId;
    private Date testSignalTime;

    private Random random = new Random();

    /**
     * 测试前准备工作
     */
    @BeforeEach
    public void setUp() {
        // 创建测试用车辆
        Vehicle vehicle = new Vehicle();
        vehicle.setVid(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        vehicle.setBatteryType("三元电池");
        vehicleService.save(vehicle);
        testCarId = vehicle.getCarId();

        // 创建测试用告警规则
        WarnRule rule = new WarnRule();
        rule.setWarnId(random.nextInt(90000) + 10000); // 随机生成5位数的告警规则ID
        rule.setWarnName("测试规则");
        rule.setBatteryType("三元电池");
        rule.setRule("{\"leftOperand\":\"Mx\",\"rightOperand\":\"Mi\",\"operator\":1,\"rules\":[{\"minValue\":5.0,\"maxValue\":null,\"includeMin\":true,\"includeMax\":false,\"level\":0}]}");
        warnRuleService.save(rule);
        testWarnId = rule.getWarnId();

        // 创建测试用电池信号
        testSignalTime = new Date();
        BatterySignal signal = new BatterySignal();
        signal.setCarId(testCarId);
        signal.setWarnId(testWarnId);
        signal.setSignalData("{\"Mx\":12.5,\"Mi\":0.5,\"Ix\":10.0,\"Ii\":9.5}");
        signal.setSignalTime(testSignalTime);
        signal.setProcessed(false); // 初始状态：未处理
        signal.setProcessTime(null);
        batterySignalService.save(signal);
        testSignalId = signal.getId();
    }

    /**
     * 测试保存信号
     */
    @Test
    @Transactional
    public void testSave() {
        BatterySignal signal = new BatterySignal();
        signal.setCarId(testCarId);
        signal.setWarnId(testWarnId);
        signal.setSignalData("{\"Mx\":13.5,\"Mi\":0.6,\"Ix\":11.0,\"Ii\":9.0}");
        signal.setSignalTime(new Date());
        
        boolean result = batterySignalService.save(signal);
        
        assertTrue(result, "保存信号应该成功");
        assertNotNull(signal.getId(), "信号ID不应为空");
    }

    /**
     * 测试标记信号为处理中
     */
    @Test
    @Transactional
    public void testMarkSignalProcessing() {
        // 标记为处理中
        boolean result = batterySignalService.markSignalProcessing(testSignalId);
        
        // 验证结果
        assertTrue(result, "标记为处理中应该成功");
        
        // 获取更新后的信号
        BatterySignal signal = batterySignalService.getById(testSignalId);
        
        // 验证状态
        assertTrue(signal.getProcessed(), "processed标志应该为true");
        assertNull(signal.getProcessTime(), "processTime应该为null");
    }
    
    /**
     * 测试标记信号为已处理完成
     */
    @Test
    @Transactional
    public void testMarkSignalProcessed() {
        // 先标记为处理中
        batterySignalService.markSignalProcessing(testSignalId);
        
        // 标记为处理完成
        boolean result = batterySignalService.markSignalProcessed(testSignalId);
        
        // 验证结果
        assertTrue(result, "标记为处理完成应该成功");
        
        // 获取更新后的信号
        BatterySignal signal = batterySignalService.getById(testSignalId);
        
        // 验证状态
        assertTrue(signal.getProcessed(), "processed标志应该为true");
        assertNotNull(signal.getProcessTime(), "processTime应该有值");
    }
    
    /**
     * 测试重置信号状态
     */
    @Test
    @Transactional
    public void testResetSignalStatus() {
        // 先标记为处理中
        batterySignalService.markSignalProcessing(testSignalId);
        
        // 重置状态
        boolean result = batterySignalService.resetSignalStatus(testSignalId);
        
        // 验证结果
        assertTrue(result, "重置状态应该成功");
        
        // 获取更新后的信号
        BatterySignal signal = batterySignalService.getById(testSignalId);
        
        // 验证状态
        assertFalse(signal.getProcessed(), "processed标志应该为false");
        assertNull(signal.getProcessTime(), "processTime应该为null");
    }
    
    /**
     * 测试查询卡在处理中状态的信号
     */
    @Test
    @Transactional
    public void testFindStuckSignals() {
        // 先标记为处理中
        batterySignalService.markSignalProcessing(testSignalId);
        
        // 设置信号时间为足够早，使其被判定为卡住
        BatterySignal signal = batterySignalService.getById(testSignalId);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -10);  // 10分钟前
        signal.setSignalTime(calendar.getTime());
        batterySignalService.updateById(signal);
        
        // 查询卡在处理中的信号
        List<BatterySignal> stuckSignals = batterySignalService.findStuckSignals(5);  // 超过5分钟视为卡住
        
        // 验证结果
        assertTrue(stuckSignals.stream().anyMatch(s -> s.getId().equals(testSignalId)), 
            "应该能找到卡在处理中状态的测试信号");
    }
    
    /**
     * 测试查询未处理的信号
     */
    @Test
    @Transactional
    public void testFindUnprocessedSignals() {
        // 创建一个新的未处理信号
        BatterySignal signal = new BatterySignal();
        signal.setCarId(testCarId);
        signal.setWarnId(testWarnId);
        signal.setSignalData("{\"Mx\":13.5,\"Mi\":0.6,\"Ix\":11.0,\"Ii\":10.5}");
        signal.setSignalTime(new Date());
        signal.setProcessed(false);
        signal.setProcessTime(null);
        batterySignalService.save(signal);
        
        // 查询未处理的信号
        List<BatterySignal> unprocessedSignals = batterySignalService.findUnprocessedSignals(10);
        
        // 验证结果
        assertFalse(unprocessedSignals.isEmpty(), "应该能找到未处理的信号");
        assertTrue(unprocessedSignals.stream().anyMatch(s -> s.getId().equals(signal.getId()) || s.getId().equals(testSignalId)),
            "结果应包含测试信号");
    }
    
    /**
     * 测试查询近期未处理的信号
     */
    @Test
    @Transactional
    public void testFindRecentUnprocessedSignals() {
        // 查询最近30分钟内的未处理信号
        List<BatterySignal> recentSignals = batterySignalService.findRecentUnprocessedSignals(30, 10);
        
        // 验证结果
        assertFalse(recentSignals.isEmpty(), "应该能找到近期未处理的信号");
        assertTrue(recentSignals.stream().anyMatch(s -> s.getId().equals(testSignalId)),
            "结果应包含测试信号");
    }
    
    /**
     * 测试按时间范围查询未处理的信号
     */
    @Test
    @Transactional
    public void testFindUnprocessedSignalsByTimeRange() {
        // 设置时间范围
        Calendar startCal = Calendar.getInstance();
        startCal.add(Calendar.HOUR, -2);
        
        Calendar endCal = Calendar.getInstance();
        endCal.add(Calendar.HOUR, 1);
        
        // 查询指定时间范围内的未处理信号
        List<BatterySignal> rangeSignals = batterySignalService.findUnprocessedSignalsByTimeRange(
            startCal.getTime(), endCal.getTime(), 10);
        
        // 验证结果
        assertFalse(rangeSignals.isEmpty(), "应该能找到时间范围内未处理的信号");
        assertTrue(rangeSignals.stream().anyMatch(s -> s.getId().equals(testSignalId)),
            "结果应包含测试信号");
    }
    
    /**
     * 测试信号处理的完整流程（三状态转换）
     */
    @Test
    @Transactional
    public void testSignalProcessFlow() {
        // 1. 初始状态：未处理
        BatterySignal initialSignal = batterySignalService.getById(testSignalId);
        assertFalse(initialSignal.getProcessed(), "初始应为未处理状态");
        assertNull(initialSignal.getProcessTime(), "初始processTime应为null");
        
        // 2. 标记为处理中
        batterySignalService.markSignalProcessing(testSignalId);
        BatterySignal processingSignal = batterySignalService.getById(testSignalId);
        assertTrue(processingSignal.getProcessed(), "应标记为处理中状态");
        assertNull(processingSignal.getProcessTime(), "处理中状态的processTime应为null");
        
        // 3. 标记为处理完成
        batterySignalService.markSignalProcessed(testSignalId);
        BatterySignal processedSignal = batterySignalService.getById(testSignalId);
        assertTrue(processedSignal.getProcessed(), "应标记为已处理状态");
        assertNotNull(processedSignal.getProcessTime(), "处理完成后processTime应有值");
    }
    
    /**
     * 测试解析信号数据
     */
    @Test
    public void testParseSignalData() {
        String signalData = "{\"Mx\":12.5,\"Mi\":0.5,\"Ix\":10.0,\"Ii\":9.5}";
        Map<String, Object> parsedData = batterySignalService.parseSignalData(signalData);
        
        assertEquals(4, parsedData.size(), "应该有4个字段");
        assertEquals(12.5, Double.parseDouble(parsedData.get("Mx").toString()), 0.001, "Mx值应正确");
        assertEquals(0.5, Double.parseDouble(parsedData.get("Mi").toString()), 0.001, "Mi值应正确");
        assertEquals(10.0, Double.parseDouble(parsedData.get("Ix").toString()), 0.001, "Ix值应正确");
        assertEquals(9.5, Double.parseDouble(parsedData.get("Ii").toString()), 0.001, "Ii值应正确");
    }
    
    /**
     * 测试解析空或无效的信号数据
     */
    @Test
    public void testParseInvalidSignalData() {
        // 测试空数据
        Map<String, Object> emptyResult = batterySignalService.parseSignalData(null);
        assertTrue(emptyResult.isEmpty(), "空数据应返回空Map");
        
        // 测试空字符串
        Map<String, Object> emptyStringResult = batterySignalService.parseSignalData("");
        assertTrue(emptyStringResult.isEmpty(), "空字符串应返回空Map");
        
        // 测试无效JSON
        Map<String, Object> invalidResult = batterySignalService.parseSignalData("{invalid_json");
        assertTrue(invalidResult.isEmpty(), "无效JSON应返回空Map");
    }
    
    /**
     * 测试按车辆ID分页查询信号
     */
    @Test
    @Transactional
    public void testPageByCarId() {
        // 创建多个信号
        for (int i = 0; i < 15; i++) {
            BatterySignal signal = new BatterySignal();
            signal.setCarId(testCarId);
            signal.setWarnId(testWarnId);
            signal.setSignalData(createSignalJson(12.0 + i, 0.5, 10.0, 9.0));
            signal.setSignalTime(new Date(System.currentTimeMillis() - i * 60000)); // 不同时间
            batterySignalService.save(signal);
        }
        
        // 分页查询
        IPage<BatterySignal> page = batterySignalService.pageByCarId(testCarId, 1, 10);
        
        // 验证结果
        assertEquals(1, page.getCurrent(), "当前页应为1");
        assertEquals(10, page.getSize(), "每页大小应为10");
        assertEquals(10, page.getRecords().size(), "第一页应有10条记录");
        assertTrue(page.getTotal() >= 15, "总记录数应至少为15");
        
        // 验证排序（按信号时间降序）
        List<BatterySignal> records = page.getRecords();
        for (int i = 0; i < records.size() - 1; i++) {
            assertTrue(records.get(i).getSignalTime().compareTo(records.get(i + 1).getSignalTime()) >= 0,
                "记录应按信号时间降序排列");
        }
    }
    
    /**
     * 测试清理历史数据
     */
    @Test
    @Transactional
    public void testCleanHistoryData() {
        // 创建一些较早的信号数据
        for (int i = 0; i < 5; i++) {
            BatterySignal signal = new BatterySignal();
            signal.setCarId(testCarId);
            signal.setWarnId(testWarnId);
            signal.setSignalData(createSignalJson(11.0 + i, 0.5, 10.0, 9.0));
            
            // 设置为10天前的数据
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -10);
            signal.setSignalTime(calendar.getTime());
            
            batterySignalService.save(signal);
        }
        
        // 设置截止日期为5天前
        Calendar cutoffCal = Calendar.getInstance();
        cutoffCal.add(Calendar.DAY_OF_MONTH, -5);
        Date cutoffDate = cutoffCal.getTime();
        
        // 清理历史数据
        boolean result = batterySignalService.cleanHistoryData(testCarId, cutoffDate);
        
        // 验证结果
        assertTrue(result, "清理历史数据应该成功");
        
        // 验证5天前的数据已被清理
        List<BatterySignal> recentSignals = batterySignalService.findRecentUnprocessedSignals(30 * 24 * 60, 100);
        for (BatterySignal signal : recentSignals) {
            if (signal.getCarId().equals(testCarId)) {
                assertFalse(signal.getSignalTime().before(cutoffDate), 
                    "车辆" + testCarId + "的信号时间应晚于截止日期");
            }
        }
    }

    /**
     * 创建测试用信号数据JSON
     */
    private String createSignalJson(double mx, double mi, double ix, double ii) {
        return String.format("{\"Mx\":%.1f,\"Mi\":%.1f,\"Ix\":%.1f,\"Ii\":%.1f}", mx, mi, ix, ii);
    }
}