//package com.example.xiangyuzhao.service;
//
//import com.baomidou.mybatisplus.core.metadata.IPage;
//import com.example.xiangyuzhao.entity.BatterySignal;
//import com.example.xiangyuzhao.entity.Vehicle;
//import com.example.xiangyuzhao.entity.WarnRule;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * BatterySignalService单元测试
// */
//@SpringBootTest
//public class BatterySignalServiceTest {
//
//    @Autowired
//    private BatterySignalService batterySignalService;
//
//    @Autowired
//    private VehicleService vehicleService;
//
//    @Autowired
//    private WarnRuleService warnRuleService;
//
//    private Integer testCarId;
//    private Integer testWarnId;
//    private Long testSignalId;
//    private Date testSignalTime;
//
//    private Random random = new Random();
//    private int baseWarnId = 10000; // 使用一个较大的基础ID，避免与已有数据冲突
//
//    /**
//     * 测试前准备工作
//     */
//    @BeforeEach
//    public void setUp() {
//        // 创建测试用车辆
//        Vehicle vehicle = new Vehicle();
//        vehicle.setVid(UUID.randomUUID().toString().substring(0, 16));
//        vehicle.setBatteryType("三元锂");
//        vehicleService.save(vehicle);
//        testCarId = vehicle.getCarId();
//
//        // 创建测试用告警规则
//        WarnRule rule = new WarnRule();
//        rule.setWarnId(new Random().nextInt(90000) + 10000); // 随机生成5位数的告警规则ID
//        rule.setWarnName("测试告警规则" + rule.getWarnId());
//        rule.setBatteryType("三元锂");
//
//        // 设置规则JSON
//        String ruleJson = "{\"leftOperand\":\"Mx\",\"rightOperand\":\"Mi\",\"operator\":1,\"rules\":[" +
//                "{\"minValue\":0.5,\"maxValue\":1.0,\"includeMin\":true,\"includeMax\":false,\"level\":1}" +
//                "]}";
//        rule.setRule(ruleJson);
//
//        warnRuleService.save(rule);
//        testWarnId = rule.getWarnId();
//    }
//
//    /**
//     * 向数据库插入一条测试数据
//     */
//    private void insertTestData() {
//        // 保存一条电池信号记录
//        testSignalTime = new Date();
//        BatterySignal signal = new BatterySignal(
//            testCarId,
//            testWarnId,
//            "{\"Mx\":12.0,\"Mi\":0.5,\"Ix\":10.0,\"Ii\":9.0}",
//            testSignalTime
//        );
//        batterySignalService.save(signal);
//        testSignalId = signal.getId();
//    }
//
//    /**
//     * 创建测试用告警规则，确保ID唯一性
//     */
//    private Integer createTestWarnRule() {
//        // 使用UUID的hashCode作为warnId的基础，确保更好的唯一性
//        int warnId = baseWarnId + Math.abs(UUID.randomUUID().hashCode() % 90000);
//
//        // 检查ID是否已存在
//        List<WarnRule> existingRules = warnRuleService.listByBatteryType("三元电池");
//        boolean idExists;
//        do {
//            idExists = false;
//            for (WarnRule rule : existingRules) {
//                if (rule.getWarnId() != null && rule.getWarnId().equals(warnId)) {
//                    idExists = true;
//                    warnId++;
//                    break;
//                }
//            }
//        } while (idExists);
//
//        WarnRule warnRule = new WarnRule();
//        warnRule.setWarnId(warnId);
//        warnRule.setWarnName("测试告警规则" + warnId);
//        warnRule.setBatteryType("三元电池");
//        warnRule.setRule("{\"leftOperand\":\"Mx\",\"rightOperand\":\"Mi\",\"operator\":1,\"rules\":[{\"minValue\":0.5,\"maxValue\":1.0,\"includeMin\":true,\"includeMax\":false,\"level\":1}]}");
//        warnRuleService.save(warnRule);
//        return warnRule.getWarnId();
//    }
//
//    /**
//     * 测试保存电池信号
//     */
//    @Test
//    @Transactional
//    public void testSave() {
//        // 创建测试用告警规则
//        Integer testWarnId = createTestWarnRule();
//
//        // 创建电池信号对象，使用构造函数
//        Date testDate = new Date();
//        BatterySignal signal = new BatterySignal(
//            testCarId,
//            testWarnId,
//            createSignalJson(12.5, 0.5, 10.0, 9.5),
//            testDate
//        );
//
//        // 保存
//        boolean result = batterySignalService.save(signal);
//
//        // 验证
//        assertTrue(result, "保存电池信号应该成功");
//        assertNotNull(signal.getId(), "保存后应该生成ID");
//        assertEquals(testDate, signal.getSignalTime(), "信号时间应该与设置的时间一致");
//    }
//
//    /**
//     * 测试自动设置的信号时间
//     */
//    @Test
//    @Transactional
//    public void testAutoSetSignalTime() {
//        // 创建测试用告警规则
//        Integer testWarnId = createTestWarnRule();
//
//        // 创建电池信号对象，不指定时间，应该自动设置当前时间
//        BatterySignal signal = new BatterySignal(
//            testCarId,
//            testWarnId,
//            createSignalJson(12.5, 0.5, 10.0, 9.5)
//        );
//
//        // 记录保存前的时间
//        Date beforeSave = new Date();
//
//        // 保存
//        boolean result = batterySignalService.save(signal);
//
//        // 记录保存后的时间
//        Date afterSave = new Date();
//
//        // 验证
//        assertTrue(result, "保存电池信号应该成功");
//        assertNotNull(signal.getId(), "保存后应该生成ID");
//        assertNotNull(signal.getSignalTime(), "信号时间不应为null");
//
//        // 验证自动设置的时间，允许2秒的时间差
//        long beforeMillis = beforeSave.getTime();
//        long afterMillis = afterSave.getTime();
//        long signalMillis = signal.getSignalTime().getTime();
//
//        assertTrue(signalMillis >= beforeMillis - 2000 && signalMillis <= afterMillis + 2000,
//                  "信号时间应该在保存操作前后2秒范围内");
//    }
//
//    /**
//     * 测试批量保存电池信号
//     */
//    @Test
//    @Transactional
//    public void testSaveList() {
//        // 创建多个电池信号对象
//        List<BatterySignal> signals = new ArrayList<>();
//        int count = 30;
//
//        for (int i = 0; i < count; i++) {
//            // 为每个信号创建唯一的告警规则
//            Integer warnId = createTestWarnRule();
//
//            // 使用构造函数
//            BatterySignal signal = new BatterySignal(
//                testCarId,
//                warnId,
//                createSignalJson(12.0 + i * 0.1, 0.5 - i * 0.01, 10.0, 9.0)
//            );
//            signals.add(signal);
//        }
//
//        // 批量保存
//        boolean result = batterySignalService.saveBatch(signals);
//
//        // 验证
//        assertTrue(result, "批量保存电池信号应该成功");
//
//        // 查询车辆的所有信号
//        List<BatterySignal> found = batterySignalService.listByCarId(testCarId);
//        assertTrue(found.size() >= count, "应该查询到至少" + count + "条记录");
//
//        // 验证每个信号都有时间
//        for (BatterySignal signal : found) {
//            assertNotNull(signal.getSignalTime(), "所有信号都应该有时间字段");
//        }
//    }
//
//    /**
//     * 测试根据ID查询信号数据
//     */
//    @Test
//    @Transactional
//    public void testGetById() {
//        insertTestData();
//
//        BatterySignal signal = batterySignalService.getById(testSignalId);
//
//        assertNotNull(signal, "应该能查询到保存的信号数据");
//        assertEquals(testCarId, signal.getCarId(), "车辆ID应该一致");
//        assertEquals(testWarnId, signal.getWarnId(), "预警ID应该一致");
//
//        // 使用Jackson或其他方式规范化JSON格式，以确保格式一致性
//        Map<String, Object> expectedData = batterySignalService.parseSignalData("{\"Mx\":12.0,\"Mi\":0.5,\"Ix\":10.0,\"Ii\":9.0}");
//        Map<String, Object> actualData = batterySignalService.parseSignalData(signal.getSignalData());
//
//        // 将数据转换为Double后比较，使用delta值处理精度问题
//        assertEquals(Double.parseDouble(expectedData.get("Mx").toString()),
//                     Double.parseDouble(actualData.get("Mx").toString()),
//                     0.001, "电压最大值应一致");
//        assertEquals(Double.parseDouble(expectedData.get("Mi").toString()),
//                     Double.parseDouble(actualData.get("Mi").toString()),
//                     0.001, "电压最小值应一致");
//        assertEquals(Double.parseDouble(expectedData.get("Ix").toString()),
//                     Double.parseDouble(actualData.get("Ix").toString()),
//                     0.001, "电流最大值应一致");
//        assertEquals(Double.parseDouble(expectedData.get("Ii").toString()),
//                     Double.parseDouble(actualData.get("Ii").toString()),
//                     0.001, "电流最小值应一致");
//
//        // 允许时间有1秒的误差范围，使用毫秒比较
//        long timeDiff = Math.abs(testSignalTime.getTime() - signal.getSignalTime().getTime());
//        assertTrue(timeDiff <= 1000, "信号时间应该与设置的时间接近，允许1秒误差");
//    }
//
//    /**
//     * 测试根据车辆ID查询电池信号列表
//     */
//    @Test
//    @Transactional
//    public void testListByCarId() {
//        // 先保存多条记录
//        int count = 10;
//        List<Date> testDates = new ArrayList<>();
//
//        for (int i = 0; i < count; i++) {
//            // 为每个信号创建唯一的告警规则
//            Integer warnId = createTestWarnRule();
//
//            // 创建递增的时间，确保每个时间戳有足够差距
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.MINUTE, i);
//            calendar.add(Calendar.SECOND, i * 10);
//            Date signalTime = calendar.getTime();
//            testDates.add(signalTime);
//
//            // 使用构造函数
//            BatterySignal signal = new BatterySignal(
//                testCarId,
//                warnId,
//                createSignalJson(12.0 + i * 0.1, 0.5, 10.0, 9.0),
//                signalTime
//            );
//            batterySignalService.save(signal);
//
//            // 添加短暂延迟，避免时间戳过于接近
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                // 忽略中断异常
//            }
//        }
//
//        // 查询
//        List<BatterySignal> list = batterySignalService.listByCarId(testCarId);
//
//        // 验证
//        assertNotNull(list, "查询结果不应为null");
//        assertTrue(list.size() >= count, "应该查询到至少" + count + "条记录");
//        assertEquals(testCarId, list.get(0).getCarId(), "车辆ID应该一致");
//
//        // 验证列表按时间降序排序，考虑到可能的其他测试数据，只验证前count-1个时间关系
//        int checkLimit = Math.min(list.size() - 1, count - 1);
//        for (int i = 0; i < checkLimit; i++) {
//            assertTrue(list.get(i).getSignalTime().getTime() >= list.get(i + 1).getSignalTime().getTime(),
//                      "列表应该按时间降序排序");
//        }
//    }
//
//    /**
//     * 测试获取车辆最新电池信号
//     */
//    @Test
//    @Transactional
//    public void testGetLatestByCarId() {
//        // 先保存多条记录，模拟时间序列数据
//        List<Date> testDates = new ArrayList<>();
//
//        for (int i = 0; i < 5; i++) {
//            // 为每个信号创建唯一的告警规则
//            Integer warnId = createTestWarnRule();
//
//            // 创建递增的时间，确保有足够的时间间隔
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.MINUTE, i * 5);
//            Date signalTime = calendar.getTime();
//            testDates.add(signalTime);
//
//            // 使用构造函数
//            BatterySignal signal = new BatterySignal(
//                testCarId,
//                warnId,
//                createSignalJson(10.0 + i, 0.5, 10.0, 9.5),
//                signalTime
//            );
//            batterySignalService.save(signal);
//
//            // 等待一段时间，确保创建时间不同
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                // 忽略中断异常
//            }
//        }
//
//        // 获取最新信号
//        BatterySignal latest = batterySignalService.getLatestByCarId(testCarId);
//
//        // 验证
//        assertNotNull(latest, "应该能查询到最新的电池信号");
//        assertEquals(testCarId, latest.getCarId(), "车辆ID应该一致");
//
//        // 验证查询到的是最新的信号，使用解析方法验证数据而非字符串比较
//        Map<String, Object> parsedData = batterySignalService.parseSignalData(latest.getSignalData());
//        assertEquals(14.0, Double.parseDouble(parsedData.get("Mx").toString()), 0.001, "最新信号应该包含正确的电压最大值");
//
//        // 验证时间是最新的（时间可能带有毫秒精度，允许1秒的误差）
//        Date expectedLatestDate = testDates.get(testDates.size() - 1);
//        long timeDiff = Math.abs(latest.getSignalTime().getTime() - expectedLatestDate.getTime());
//        assertTrue(timeDiff <= 1000, "返回的信号时间应该是最新的（允许1秒误差）");
//    }
//
//    /**
//     * 测试分页查询电池信号
//     */
//    @Test
//    @Transactional
//    public void testPageByCarId() {
//        // 先保存大量记录，以测试分页
//        int totalRecords = 50;
//        int pageSize = 10;
//
//        for (int i = 0; i < totalRecords; i++) {
//            // 为每个信号创建唯一的告警规则
//            Integer warnId = createTestWarnRule();
//
//            // 创建递增的时间
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.MINUTE, i);
//            Date signalTime = calendar.getTime();
//
//            // 使用构造函数
//            BatterySignal signal = new BatterySignal(
//                testCarId,
//                warnId,
//                createSignalJson(12.0 + i * 0.1, 0.5 - i * 0.01, 10.0 + i * 0.05, 9.0 + i * 0.02),
//                signalTime
//            );
//            batterySignalService.save(signal);
//        }
//
//        // 分页查询第1页
//        IPage<BatterySignal> page1 = batterySignalService.pageByCarId(testCarId, 1, pageSize);
//
//        // 验证第1页
//        assertNotNull(page1, "第1页查询结果不应为null");
//        assertEquals(1, page1.getCurrent(), "当前页应该是第1页");
//        assertEquals(pageSize, page1.getSize(), "页大小应该是" + pageSize);
//        assertEquals(pageSize, page1.getRecords().size(), "第1页应该有" + pageSize + "条记录");
//        assertEquals(totalRecords, page1.getTotal(), "总记录数应该是" + totalRecords);
//
//        // 验证第1页数据是按时间降序排序的
//        for (int i = 0; i < page1.getRecords().size() - 1; i++) {
//            assertTrue(page1.getRecords().get(i).getSignalTime().compareTo(
//                      page1.getRecords().get(i + 1).getSignalTime()) >= 0,
//                      "列表应该按时间降序排序");
//        }
//
//        // 分页查询第3页
//        IPage<BatterySignal> page3 = batterySignalService.pageByCarId(testCarId, 3, pageSize);
//
//        // 验证第3页
//        assertNotNull(page3, "第3页查询结果不应为null");
//        assertEquals(3, page3.getCurrent(), "当前页应该是第3页");
//        assertEquals(pageSize, page3.getSize(), "页大小应该是" + pageSize);
//        assertEquals(pageSize, page3.getRecords().size(), "第3页应该有" + pageSize + "条记录");
//    }
//
//    /**
//     * 测试更新信号数据
//     */
//    @Test
//    @Transactional
//    public void testUpdateById() {
//        insertTestData();
//
//        // 先查出来
//        BatterySignal signal = batterySignalService.getById(testSignalId);
//        Date originalTime = signal.getSignalTime();
//
//        // 更新数据，但不更新时间
//        String newSignalData = "{\"Mx\":12.0,\"Mi\":0.5,\"Ix\":10.0,\"Ii\":9.0}";
//        signal.setSignalData(newSignalData);
//        boolean result = batterySignalService.updateById(signal);
//
//        // 验证更新成功
//        assertTrue(result, "更新操作应该成功");
//
//        // 再查询
//        BatterySignal updated = batterySignalService.getById(testSignalId);
//
//        assertEquals(testSignalId, updated.getId(), "ID不应该变化");
//        assertEquals(testCarId, updated.getCarId(), "车辆ID不应该变化");
//        assertEquals(testWarnId, updated.getWarnId(), "预警ID不应该变化");
//
//        // 不直接比较字符串，而是比较解析后的JSON内容
//        Map<String, Object> expectedData = batterySignalService.parseSignalData(newSignalData);
//        Map<String, Object> actualData = batterySignalService.parseSignalData(updated.getSignalData());
//
//        // 比较两个Map的值
//        assertEquals(expectedData.size(), actualData.size(), "信号数据字段数量应该相同");
//        for (String key : expectedData.keySet()) {
//            assertEquals(Double.parseDouble(expectedData.get(key).toString()),
//                         Double.parseDouble(actualData.get(key).toString()),
//                         0.001,
//                         "信号数据字段 " + key + " 的值应该相同");
//        }
//
//        // 验证时间没有变化，允许1秒的误差
//        long timeDiff = Math.abs(originalTime.getTime() - updated.getSignalTime().getTime());
//        assertTrue(timeDiff <= 1000, "信号时间不应变化（允许1秒误差）");
//    }
//
//    /**
//     * 测试删除电池信号
//     */
//    @Test
//    @Transactional
//    public void testRemoveById() {
//        // 创建测试用告警规则
//        Integer testWarnId = createTestWarnRule();
//
//        // 先保存一条记录
//        BatterySignal signal = new BatterySignal(
//            testCarId,
//            testWarnId,
//            createSignalJson(12.5, 0.5, 10.0, 9.5)
//        );
//        batterySignalService.save(signal);
//
//        Long id = signal.getId();
//
//        // 删除
//        boolean result = batterySignalService.removeById(id);
//
//        // 验证
//        assertTrue(result, "删除电池信号应该成功");
//        assertNull(batterySignalService.getById(id), "删除后应该查询不到记录");
//    }
//
//    /**
//     * 测试批量删除电池信号
//     */
//    @Test
//    @Transactional
//    public void testRemoveBatchByIds() {
//        // 先保存多条记录
//        List<Long> ids = new ArrayList<>();
//        int count = 20;
//
//        for (int i = 0; i < count; i++) {
//            // 为每个信号创建唯一的告警规则
//            Integer warnId = createTestWarnRule();
//
//            BatterySignal signal = new BatterySignal(
//                testCarId,
//                warnId,
//                createSignalJson(12.0 + i * 0.2, 0.5, 10.0, 9.0)
//            );
//            batterySignalService.save(signal);
//            ids.add(signal.getId());
//        }
//
//        // 批量删除前10条
//        List<Long> idsToDelete = ids.subList(0, 10);
//        boolean result = batterySignalService.removeBatchByIds(idsToDelete);
//
//        // 验证
//        assertTrue(result, "批量删除电池信号应该成功");
//        for (Long id : idsToDelete) {
//            assertNull(batterySignalService.getById(id), "删除后应该查询不到记录");
//        }
//
//        // 验证剩余记录
//        List<BatterySignal> remainingSignals = batterySignalService.listByCarId(testCarId);
//        assertEquals(count - 10, remainingSignals.size(), "应该剩余" + (count - 10) + "条记录");
//    }
//
//    /**
//     * 测试清除历史数据
//     */
//    @Test
//    @Transactional
//    public void testCleanHistoryData() {
//        // 先删除现有测试车辆的信号数据
//        List<BatterySignal> existingSignals = batterySignalService.listByCarId(testCarId);
//        for (BatterySignal signal : existingSignals) {
//            batterySignalService.removeById(signal.getId());
//        }
//
//        // 验证清除成功
//        assertEquals(0, batterySignalService.listByCarId(testCarId).size(), "清除后应该没有任何信号记录");
//
//        // 先保存一组时间跨度为25天的数据，每天一条
//        int days = 25;
//        for (int i = days; i >= 0; i--) {
//            // 创建过去i天的时间
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.DAY_OF_MONTH, -i);
//            Date pastDate = calendar.getTime();
//
//            BatterySignal signal = new BatterySignal(
//                testCarId,
//                testWarnId,
//                "{\"Mx\":12.0,\"Mi\":0.5,\"Ix\":10.0,\"Ii\":9.0}",
//                pastDate
//            );
//            batterySignalService.save(signal);
//        }
//
//        // 验证插入成功
//        assertEquals(days + 1, batterySignalService.listByCarId(testCarId).size(), "应该成功插入26条数据");
//
//        // 清除10天之前的数据（保留10天内的数据）
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.DAY_OF_MONTH, -10);
//        Date tenDaysAgo = calendar.getTime();
//        batterySignalService.cleanHistoryData(testCarId, tenDaysAgo);
//
//        // 应该保留10条记录（当天加上过去9天，共10条）
//        // 由于cleanHistoryData使用 < 符号，不包含边界日期，所以是10天的数据而不是11天
//        int expectedCount = 10; // 当前实现是保留从今天到10天前（不含第10天）的记录，共10条
//        assertEquals(expectedCount, batterySignalService.listByCarId(testCarId).size(),
//                "应该只剩下" + expectedCount + "天内的信号");
//    }
//
//    /**
//     * 测试解析信号数据
//     */
//    @Test
//    public void testParseSignalData() {
//        String signalData = createSignalJson(12.5, 0.5, 10.0, 9.5);
//
//        // 解析
//        Map<String, Object> parsed = batterySignalService.parseSignalData(signalData);
//
//        // 验证
//        assertNotNull(parsed, "解析结果不应为null");
//        assertEquals(4, parsed.size(), "解析结果应该有4个字段");
//        assertEquals(12.5, Double.parseDouble(parsed.get("Mx").toString()), 0.001, "电压最大值应该正确");
//        assertEquals(0.5, Double.parseDouble(parsed.get("Mi").toString()), 0.001, "电压最小值应该正确");
//        assertEquals(10.0, Double.parseDouble(parsed.get("Ix").toString()), 0.001, "电流最大值应该正确");
//        assertEquals(9.5, Double.parseDouble(parsed.get("Ii").toString()), 0.001, "电流最小值应该正确");
//    }
//
//    /**
//     * 测试大批量数据处理
//     */
//    @Test
//    @Transactional
//    public void testLargeDataVolume() {
//        // 创建大量电池信号对象
//        List<BatterySignal> signals = new ArrayList<>();
//        int count = 100; // 大数据量测试
//
//        for (int i = 0; i < count; i++) {
//            // 为每个信号创建唯一的告警规则
//            Integer warnId = createTestWarnRule();
//
//            // 创建递增的时间
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.MINUTE, i);
//
//            // 使用构造函数
//            BatterySignal signal = new BatterySignal(
//                testCarId,
//                warnId,
//                createSignalJson(12.0 + i * 0.01, 0.5 - i * 0.001, 10.0 + i * 0.005, 9.0 + i * 0.002),
//                calendar.getTime()
//            );
//            signals.add(signal);
//        }
//
//        // 批量保存
//        long startTime = System.currentTimeMillis();
//        boolean result = batterySignalService.saveList(signals);
//        long endTime = System.currentTimeMillis();
//
//        // 验证
//        assertTrue(result, "大批量保存电池信号应该成功");
//        System.out.println("保存" + count + "条记录耗时: " + (endTime - startTime) + "ms");
//
//        // 验证数据数量
//        List<BatterySignal> found = batterySignalService.listByCarId(testCarId);
//        assertEquals(count, found.size(), "应该查询到" + count + "条记录");
//
//        // 测试分页查询大数据量的性能
//        startTime = System.currentTimeMillis();
//        IPage<BatterySignal> page = batterySignalService.pageByCarId(testCarId, 1, 20);
//        endTime = System.currentTimeMillis();
//
//        System.out.println("分页查询" + count + "条记录中的20条耗时: " + (endTime - startTime) + "ms");
//        assertEquals(20, page.getRecords().size(), "第1页应该有20条记录");
//    }
//
//    /**
//     * 测试唯一键约束 (car_id, warn_id, signal_time)
//     */
//    @Test
//    @Transactional
//    public void testUniqueConstraint() {
//        // 创建测试用告警规则
//        Integer testWarnId = createTestWarnRule();
//
//        // 创建一个固定的时间
//        Calendar calendar = Calendar.getInstance();
//        Date fixedTime = calendar.getTime();
//
//        // 第一次保存
//        BatterySignal signal1 = new BatterySignal(
//            testCarId,
//            testWarnId,
//            createSignalJson(12.5, 0.5, 10.0, 9.5),
//            fixedTime
//        );
//        boolean result1 = batterySignalService.save(signal1);
//        assertTrue(result1, "第一次保存应该成功");
//
//        // 创建一个不同的时间
//        calendar.add(Calendar.SECOND, 1);
//        Date differentTime = calendar.getTime();
//
//        // 第二次保存，使用相同的car_id和warn_id，但不同的signal_time
//        BatterySignal signal2 = new BatterySignal(
//            testCarId,
//            testWarnId,
//            createSignalJson(13.5, 0.6, 11.0, 10.5),
//            differentTime
//        );
//        boolean result2 = batterySignalService.save(signal2);
//        assertTrue(result2, "使用不同的时间应该可以保存相同的car_id和warn_id");
//
//        // 验证两条记录都存在
//        List<BatterySignal> signals = batterySignalService.listByCarId(testCarId);
//        assertTrue(signals.size() >= 2, "应该至少有两条记录");
//
//        // 验证可以通过ID区分它们
//        assertNotEquals(signal1.getId(), signal2.getId(), "两条记录的ID应该不同");
//    }
//
//    /**
//     * 创建信号JSON字符串
//     */
//    private String createSignalJson(double mx, double mi, double ix, double ii) {
//        return String.format("{\"Mx\":%.1f,\"Mi\":%.1f,\"Ix\":%.1f,\"Ii\":%.1f}", mx, mi, ix, ii);
//    }
//}