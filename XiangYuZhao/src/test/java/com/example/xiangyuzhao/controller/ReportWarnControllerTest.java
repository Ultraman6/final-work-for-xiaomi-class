package com.example.xiangyuzhao.controller;

import com.example.xiangyuzhao.dto.req.BatterySignalUploadReq;
import com.example.xiangyuzhao.dto.resp.BatteryWarnResp;
import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.entity.Vehicle;
import com.example.xiangyuzhao.entity.WarnInfo;
import com.example.xiangyuzhao.entity.WarnRule;
import com.example.xiangyuzhao.service.BatterySignalService;
import com.example.xiangyuzhao.service.VehicleService;
import com.example.xiangyuzhao.service.WarnInfoService;
import com.example.xiangyuzhao.service.WarnRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportWarnControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BatterySignalService batterySignalService;

    @Mock
    private VehicleService vehicleService;

    @Mock
    private WarnInfoService warnInfoService;

    @Mock
    private WarnRuleService warnRuleService;

    @InjectMocks
    private ReportWarnController reportWarnController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(reportWarnController).build();
    }

    @Test
    void testProcessWarnings() throws Exception {
        // Prepare test data
        BatterySignalUploadReq req = new BatterySignalUploadReq();
        req.setCarId(101);
        req.setVid("TEST001");
        req.setWarnId(201);
        Map<String, Object> signalMap = new HashMap<>();
        signalMap.put("temperature", 65);
        signalMap.put("voltage", 3.8);
        req.setSignal(signalMap);
        req.setSignalTime(new Date());
        List<BatterySignalUploadReq> requests = Collections.singletonList(req);

        // Prepare vehicle data
        Vehicle vehicle = new Vehicle();
        vehicle.setCarId(101);
        vehicle.setVid("TEST001");

        // Prepare signal data
        BatterySignal signal = new BatterySignal();
        signal.setId(1L);
        signal.setCarId(101);
        signal.setWarnId(201);
        signal.setSignalData("{\"temperature\": 65, \"voltage\": 3.8}");
        signal.setSignalTime(req.getSignalTime());

        // Prepare parsed signal data
        Map<String, Object> parsedSignal = new HashMap<>(signalMap);

        // Prepare warnings
        WarnInfo warnInfo = new WarnInfo();
        warnInfo.setId(1L);
        warnInfo.setCarId(101);
        warnInfo.setWarnId(201);
        warnInfo.setWarnName("电池温度过高");
        warnInfo.setWarnLevel(1);
        warnInfo.setSignalId(1L);
        warnInfo.setSignalTime(req.getSignalTime());
        warnInfo.setWarnTime(new Date());
        List<WarnInfo> warnings = Collections.singletonList(warnInfo);

        // Mock service responses
        when(vehicleService.getById(eq(101))).thenReturn(vehicle);
        doNothing().when(batterySignalService).save(any(BatterySignal.class));
        when(batterySignalService.parseSignalData(anyString())).thenReturn(parsedSignal);
        when(warnInfoService.processSignalWarn(eq(101), anyLong())).thenReturn(warnings);
        doNothing().when(batterySignalService).updateSignalProcessed(anyLong());

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/warn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].warnings", hasSize(1)))
                .andExpect(jsonPath("$.data[0].warnings[0].carId", is(101)))
                .andExpect(jsonPath("$.data[0].warnings[0].warnId", is(201)));

        // Verify service methods were called
        verify(vehicleService, times(1)).getById(eq(101));
        verify(batterySignalService, times(1)).save(any(BatterySignal.class));
        verify(batterySignalService, times(1)).parseSignalData(anyString());
        verify(warnInfoService, times(1)).processSignalWarn(eq(101), anyLong());
        verify(batterySignalService, times(1)).updateSignalProcessed(anyLong());
    }

    @Test
    void testProcessWarningsEmptyList() throws Exception {
        // Prepare test data
        List<BatterySignalUploadReq> requests = Collections.emptyList();

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/warn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要处理的信号数据")));

        // Verify no service method was called
        verify(vehicleService, never()).getById(anyInt());
        verify(batterySignalService, never()).save(any(BatterySignal.class));
        verify(batterySignalService, never()).parseSignalData(anyString());
        verify(warnInfoService, never()).processSignalWarn(anyInt(), anyLong());
        verify(batterySignalService, never()).updateSignalProcessed(anyLong());
    }

    @Test
    void testProcessWarningsVehicleNotFound() throws Exception {
        // Prepare test data
        BatterySignalUploadReq req = new BatterySignalUploadReq();
        req.setCarId(101);
        req.setWarnId(201);
        Map<String, Object> signalMap = new HashMap<>();
        signalMap.put("temperature", 65);
        req.setSignal(signalMap);
        List<BatterySignalUploadReq> requests = Collections.singletonList(req);

        // Mock service response for non-existent vehicle
        when(vehicleService.getById(eq(101))).thenReturn(null);

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/warn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Verify vehicle service was called but no other service methods
        verify(vehicleService, times(1)).getById(eq(101));
        verify(batterySignalService, never()).save(any(BatterySignal.class));
    }

    @Test
    void testReportSignals() throws Exception {
        // Prepare test data
        BatterySignalUploadReq req = new BatterySignalUploadReq();
        req.setCarId(101);
        req.setVid("TEST001");
        req.setWarnId(201);
        Map<String, Object> signalMap = new HashMap<>();
        signalMap.put("temperature", 65);
        signalMap.put("voltage", 3.8);
        req.setSignal(signalMap);
        req.setSignalTime(new Date());
        List<BatterySignalUploadReq> requests = Collections.singletonList(req);

        // Prepare vehicle data
        Vehicle vehicle = new Vehicle();
        vehicle.setCarId(101);
        vehicle.setVid("TEST001");

        // Prepare signal data
        BatterySignal signal = new BatterySignal();
        signal.setId(1L);
        signal.setCarId(101);
        signal.setWarnId(201);
        signal.setSignalData("{\"temperature\": 65, \"voltage\": 3.8}");
        signal.setSignalTime(req.getSignalTime());

        // Mock service responses
        when(vehicleService.getById(eq(101))).thenReturn(vehicle);
        doAnswer(invocation -> {
            BatterySignal savedSignal = invocation.getArgument(0);
            savedSignal.setId(1L);
            return null;
        }).when(batterySignalService).save(any(BatterySignal.class));

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].carId", is(101)))
                .andExpect(jsonPath("$.data[0].warnId", is(201)));

        // Verify service methods were called
        verify(vehicleService, times(1)).getById(eq(101));
        verify(batterySignalService, times(1)).save(any(BatterySignal.class));
    }

    @Test
    void testReportSignalsEmptyList() throws Exception {
        // Prepare test data
        List<BatterySignalUploadReq> requests = Collections.emptyList();

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要上报的信号数据")));

        // Verify no service method was called
        verify(vehicleService, never()).getById(anyInt());
        verify(batterySignalService, never()).save(any(BatterySignal.class));
    }

    @Test
    void testSearchWarnings() throws Exception {
        // Prepare test data
        Vehicle vehicle = new Vehicle();
        vehicle.setCarId(101);
        vehicle.setVid("TEST001");

        WarnInfo warnInfo1 = new WarnInfo();
        warnInfo1.setId(1L);
        warnInfo1.setCarId(101);
        warnInfo1.setWarnId(201);
        warnInfo1.setWarnName("电池温度过高");
        warnInfo1.setWarnLevel(1);
        warnInfo1.setSignalId(301L);
        warnInfo1.setWarnTime(new Date());

        WarnInfo warnInfo2 = new WarnInfo();
        warnInfo2.setId(2L);
        warnInfo2.setCarId(101);
        warnInfo2.setWarnId(202);
        warnInfo2.setWarnName("电池电压过低");
        warnInfo2.setWarnLevel(2);
        warnInfo2.setSignalId(302L);
        warnInfo2.setWarnTime(new Date());

        List<WarnInfo> warnInfos = Arrays.asList(warnInfo1, warnInfo2);

        // Mock service responses
        when(vehicleService.getById(eq(101))).thenReturn(vehicle);
        when(warnInfoService.listByCarId(eq(101))).thenReturn(warnInfos);

        // Perform the GET request and validate response
        mockMvc.perform(get("/api/search")
                .param("car_id", "101")
                .param("handled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].carId", is(101)))
                .andExpect(jsonPath("$.data[0].warnId", is(201)));

        // Verify service methods were called
        verify(vehicleService, times(1)).getById(eq(101));
        verify(warnInfoService, times(1)).listByCarId(eq(101));
    }

    @Test
    void testSearchWarningsVehicleNotFound() throws Exception {
        // Mock service response for non-existent vehicle
        when(vehicleService.getById(eq(101))).thenReturn(null);

        // Perform the GET request and validate response
        mockMvc.perform(get("/api/search")
                .param("car_id", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", is("车辆不存在")));

        // Verify vehicle service was called but warn info service was not
        verify(vehicleService, times(1)).getById(eq(101));
        verify(warnInfoService, never()).listByCarId(anyInt());
    }

    @Test
    void testSearchWarningsWithWarnIdFilter() throws Exception {
        // Prepare test data
        Vehicle vehicle = new Vehicle();
        vehicle.setCarId(101);
        vehicle.setVid("TEST001");

        WarnInfo warnInfo1 = new WarnInfo();
        warnInfo1.setId(1L);
        warnInfo1.setCarId(101);
        warnInfo1.setWarnId(201);
        warnInfo1.setWarnName("电池温度过高");
        warnInfo1.setWarnLevel(1);
        warnInfo1.setSignalId(301L);
        warnInfo1.setWarnTime(new Date());

        WarnInfo warnInfo2 = new WarnInfo();
        warnInfo2.setId(2L);
        warnInfo2.setCarId(101);
        warnInfo2.setWarnId(202);
        warnInfo2.setWarnName("电池电压过低");
        warnInfo2.setWarnLevel(2);
        warnInfo2.setSignalId(302L);
        warnInfo2.setWarnTime(new Date());

        List<WarnInfo> warnInfos = Arrays.asList(warnInfo1, warnInfo2);

        // Mock service responses
        when(vehicleService.getById(eq(101))).thenReturn(vehicle);
        when(warnInfoService.listByCarId(eq(101))).thenReturn(warnInfos);

        // Perform the GET request with warn_id filter and validate response
        mockMvc.perform(get("/api/search")
                .param("car_id", "101")
                .param("warn_id", "201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].carId", is(101)))
                .andExpect(jsonPath("$.data[0].warnId", is(201)));

        // Verify service methods were called
        verify(vehicleService, times(1)).getById(eq(101));
        verify(warnInfoService, times(1)).listByCarId(eq(101));
    }

    @Test
    void testProcessWarningsWithoutWarnId() throws Exception {
        // Prepare test data
        BatterySignalUploadReq req = new BatterySignalUploadReq();
        req.setCarId(101);
        req.setVid("TEST001");
        // Not setting warnId to test the null case
        req.setWarnId(null);
        Map<String, Object> signalMap = new HashMap<>();
        signalMap.put("temperature", 65);
        signalMap.put("voltage", 3.8);
        req.setSignal(signalMap);
        req.setSignalTime(new Date());
        List<BatterySignalUploadReq> requests = Collections.singletonList(req);

        // Prepare vehicle data
        Vehicle vehicle = new Vehicle();
        vehicle.setCarId(101);
        vehicle.setVid("TEST001");
        vehicle.setBatteryType("LI-ION");

        // Prepare signal data
        BatterySignal signal = new BatterySignal();
        signal.setId(1L);
        signal.setCarId(101);
        // null warnId
        signal.setWarnId(null);
        signal.setSignalData("{\"temperature\": 65, \"voltage\": 3.8}");
        signal.setSignalTime(req.getSignalTime());

        // Prepare warnings rules
        WarnRule rule1 = new WarnRule();
        rule1.setWarnId(201);
        rule1.setBatteryType("LI-ION");
        rule1.setWarnName("电池温度过高");
        
        WarnRule rule2 = new WarnRule();
        rule2.setWarnId(202);
        rule2.setBatteryType("LI-ION");
        rule2.setWarnName("电池电压异常");
        
        List<WarnRule> rules = Arrays.asList(rule1, rule2);

        // Prepare parsed signal data
        Map<String, Object> parsedSignal = new HashMap<>(signalMap);
        
        // Prepare warnings for both rules
        WarnInfo warnInfo1 = new WarnInfo();
        warnInfo1.setId(1L);
        warnInfo1.setCarId(101);
        warnInfo1.setWarnId(201);
        warnInfo1.setWarnName("电池温度过高");
        warnInfo1.setWarnLevel(1);
        warnInfo1.setSignalId(1L);
        
        WarnInfo warnInfo2 = new WarnInfo();
        warnInfo2.setId(2L);
        warnInfo2.setCarId(101);
        warnInfo2.setWarnId(202);
        warnInfo2.setWarnName("电池电压异常");
        warnInfo2.setWarnLevel(2);
        warnInfo2.setSignalId(1L);
        
        List<WarnInfo> warningsRule1 = Collections.singletonList(warnInfo1);
        List<WarnInfo> warningsRule2 = Collections.singletonList(warnInfo2);
        List<WarnInfo> combinedWarnings = Arrays.asList(warnInfo1, warnInfo2);

        // Mock service calls
        when(vehicleService.getById(eq(101))).thenReturn(vehicle);
        when(batterySignalService.save(any(BatterySignal.class))).thenAnswer(invocation -> {
            BatterySignal savedSignal = invocation.getArgument(0);
            savedSignal.setId(1L);
            return true;
        });
        when(batterySignalService.parseSignalData(anyString())).thenReturn(parsedSignal);
        when(batterySignalService.updateSignalProcessed(anyLong())).thenReturn(true);
        when(batterySignalService.updateById(any(BatterySignal.class))).thenReturn(true);
        
        // Mocks for the new functionality
        when(warnRuleService.listByBatteryType("LI-ION")).thenReturn(rules);
        
        // Mock different rule processing
        when(warnInfoService.processSignalWarn(eq(101), eq(1L)))
            .thenAnswer(invocation -> {
                // This mock assumes warnId was temporarily set on the signal
                if (signal.getWarnId() == 201) {
                    return warningsRule1;
                } else if (signal.getWarnId() == 202) {
                    return warningsRule2;
                }
                return Collections.emptyList();
            });

        // Perform request and verify response
        String requestJson = objectMapper.writeValueAsString(requests);
        mockMvc.perform(post("/api/warn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].carId", is(101)))
                .andExpect(jsonPath("$.data[0].warnId").doesNotExist())
                .andExpect(jsonPath("$.data[0].warnings", hasSize(2)))
                .andExpect(jsonPath("$.data[0].hasWarnings", is(true)));

        // Verify service methods were called
        verify(vehicleService, times(1)).getById(eq(101));
        verify(batterySignalService, times(1)).save(any(BatterySignal.class));
        verify(batterySignalService, times(1)).parseSignalData(anyString());
        verify(warnRuleService, times(1)).listByBatteryType("LI-ION");
        verify(batterySignalService, times(1)).updateSignalProcessed(eq(1L));
        verify(batterySignalService, times(1)).updateById(any(BatterySignal.class));
    }

    @Test
    void testProcessWarningsWithoutWarnIdMultipleRules() throws Exception {
        // Prepare test data for car_id 3
        BatterySignalUploadReq req = new BatterySignalUploadReq();
        req.setCarId(3);
        req.setVid("TEST003");
        // Not setting warnId to test the null case
        req.setWarnId(null);
        Map<String, Object> signalMap = new HashMap<>();
        signalMap.put("temperature", 85);
        signalMap.put("voltage", 2.5);
        signalMap.put("current", 15.0);
        req.setSignal(signalMap);
        req.setSignalTime(new Date());
        List<BatterySignalUploadReq> requests = Collections.singletonList(req);

        // Prepare vehicle data
        Vehicle vehicle = new Vehicle();
        vehicle.setCarId(3);
        vehicle.setVid("TEST003");
        vehicle.setBatteryType("LFP"); // Different battery type

        // Prepare warning rules for this battery type
        WarnRule rule1 = new WarnRule();
        rule1.setId(3L);
        rule1.setWarnId(301);
        rule1.setBatteryType("LFP");
        rule1.setWarnName("高温预警");
        rule1.setRule("{\"leftOperand\":\"temperature\",\"rightOperand\":\"threshold\",\"operator\":1,\"threshold\":80}");
        
        WarnRule rule2 = new WarnRule();
        rule2.setId(4L);
        rule2.setWarnId(302);
        rule2.setBatteryType("LFP");
        rule2.setWarnName("低压预警");
        rule2.setRule("{\"leftOperand\":\"voltage\",\"rightOperand\":\"threshold\",\"operator\":2,\"threshold\":3.0}");
        
        List<WarnRule> rules = Arrays.asList(rule1, rule2);

        // Prepare warnings for each rule
        WarnInfo warnInfo1 = new WarnInfo();
        warnInfo1.setId(3L);
        warnInfo1.setCarId(3);
        warnInfo1.setWarnId(301);
        warnInfo1.setWarnName("高温预警");
        warnInfo1.setWarnLevel(1);
        warnInfo1.setSignalId(3L);
        
        WarnInfo warnInfo2 = new WarnInfo();
        warnInfo2.setId(4L);
        warnInfo2.setCarId(3);
        warnInfo2.setWarnId(302);
        warnInfo2.setWarnName("低压预警");
        warnInfo2.setWarnLevel(2);
        warnInfo2.setSignalId(4L);
        
        List<WarnInfo> warningsRule1 = Collections.singletonList(warnInfo1);
        List<WarnInfo> warningsRule2 = Collections.singletonList(warnInfo2);

        // Create signals that will be generated internally
        BatterySignal signal1 = new BatterySignal();
        signal1.setId(3L);
        signal1.setCarId(3);
        signal1.setWarnId(301);
        signal1.setSignalData(com.alibaba.fastjson.JSON.toJSONString(signalMap));
        signal1.setSignalTime(req.getSignalTime());
        
        BatterySignal signal2 = new BatterySignal();
        signal2.setId(4L);
        signal2.setCarId(3);
        signal2.setWarnId(302);
        signal2.setSignalData(com.alibaba.fastjson.JSON.toJSONString(signalMap));
        signal2.setSignalTime(req.getSignalTime());

        // Mock service calls
        when(vehicleService.getById(eq(3))).thenReturn(vehicle);
        when(warnRuleService.listByBatteryType("LFP")).thenReturn(rules);
        
        // Mocks for signal processing
        when(batterySignalService.save(any(BatterySignal.class))).thenAnswer(invocation -> {
            BatterySignal savedSignal = invocation.getArgument(0);
            if (savedSignal.getWarnId() == 301) {
                savedSignal.setId(3L);
            } else if (savedSignal.getWarnId() == 302) {
                savedSignal.setId(4L);
            }
            return true;
        });
        
        when(batterySignalService.parseSignalData(anyString())).thenReturn(signalMap);
        when(batterySignalService.updateSignalProcessed(anyLong())).thenReturn(true);
        
        // Mock warning generation based on signal ID and rule
        when(warnInfoService.processSignalWarn(eq(3), eq(3L))).thenReturn(warningsRule1);
        when(warnInfoService.processSignalWarn(eq(3), eq(4L))).thenReturn(warningsRule2);

        // Perform request and verify response
        String requestJson = objectMapper.writeValueAsString(requests);
        mockMvc.perform(post("/api/warn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].carId", is(3)))
                .andExpect(jsonPath("$.data[0].warnId").doesNotExist())
                .andExpect(jsonPath("$.data[0].warnings", hasSize(2)))
                .andExpect(jsonPath("$.data[0].hasWarnings", is(true)))
                .andExpect(jsonPath("$.data[0].warnings[0].warnId", is(301)))
                .andExpect(jsonPath("$.data[0].warnings[1].warnId", is(302)));

        // Verify service methods were called
        verify(vehicleService, times(1)).getById(eq(3));
        verify(warnRuleService, times(1)).listByBatteryType("LFP");
        verify(batterySignalService, times(2)).save(any(BatterySignal.class)); // One for each rule
        verify(batterySignalService, times(1)).parseSignalData(anyString());
        verify(warnInfoService, times(1)).processSignalWarn(eq(3), eq(3L));
        verify(warnInfoService, times(1)).processSignalWarn(eq(3), eq(4L));
        verify(batterySignalService, times(2)).updateSignalProcessed(anyLong());
    }
} 