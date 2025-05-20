package com.example.xiangyuzhao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.entity.BatterySignal;
import com.example.xiangyuzhao.service.BatterySignalService;
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
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BatterySignalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BatterySignalService batterySignalService;

    @InjectMocks
    private BatterySignalController batterySignalController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(batterySignalController).build();
    }

    @Test
    void testQuery() throws Exception {
        // Prepare test data
        List<BatterySignal> signalList = new ArrayList<>();
        BatterySignal signal = new BatterySignal();
        signal.setId(1L);
        signal.setCarId(101);
        signal.setWarnId(201);
        signal.setSignalData("{\"temperature\": 65, \"voltage\": 3.8}");
        signal.setSignalTime(new Date());
        signal.setProcessed(false);
        signalList.add(signal);

        IPage<BatterySignal> page = new Page<>(1, 10);
        page.setRecords(signalList);
        page.setTotal(1);

        // Mock service response
        when(batterySignalService.page(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

        // Perform the GET request and validate response
        mockMvc.perform(get("/api/battery_signal")
                .param("page", "1")
                .param("size", "10")
                .param("carId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].id", is(1)))
                .andExpect(jsonPath("$.data.records[0].carId", is(101)))
                .andExpect(jsonPath("$.data.records[0].warnId", is(201)))
                .andExpect(jsonPath("$.data.total", is(1)))
                .andExpect(jsonPath("$.data.size", is(10)))
                .andExpect(jsonPath("$.data.current", is(1)));

        // Verify service method was called
        verify(batterySignalService, times(1)).page(any(Page.class), any(QueryWrapper.class));
    }

    @Test
    void testInsertSingleSignal() throws Exception {
        // Prepare test data
        BatterySignal signal = new BatterySignal();
        signal.setCarId(101);
        signal.setWarnId(201);
        signal.setSignalData("{\"temperature\": 65, \"voltage\": 3.8}");
        signal.setSignalTime(new Date());
        signal.setProcessed(false);
        List<BatterySignal> signals = Collections.singletonList(signal);

        // Mock service response
        doNothing().when(batterySignalService).save(any(BatterySignal.class));

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/battery_signal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signals)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].carId", is(101)))
                .andExpect(jsonPath("$.data[0].warnId", is(201)));

        // Verify service method was called
        verify(batterySignalService, times(1)).save(any(BatterySignal.class));
    }

    @Test
    void testInsertMultipleSignals() throws Exception {
        // Prepare test data
        BatterySignal signal1 = new BatterySignal();
        signal1.setCarId(101);
        signal1.setWarnId(201);
        signal1.setSignalData("{\"temperature\": 65, \"voltage\": 3.8}");
        signal1.setSignalTime(new Date());
        signal1.setProcessed(false);

        BatterySignal signal2 = new BatterySignal();
        signal2.setCarId(102);
        signal2.setWarnId(202);
        signal2.setSignalData("{\"temperature\": 45, \"voltage\": 3.9}");
        signal2.setSignalTime(new Date());
        signal2.setProcessed(false);

        List<BatterySignal> signals = Arrays.asList(signal1, signal2);

        // Mock service response
        doNothing().when(batterySignalService).saveBatch(anyList());

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/battery_signal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signals)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].carId", is(101)))
                .andExpect(jsonPath("$.data[1].carId", is(102)));

        // Verify service method was called
        verify(batterySignalService, times(1)).saveBatch(anyList());
    }

    @Test
    void testInsertEmptyList() throws Exception {
        // Prepare test data
        List<BatterySignal> signals = Collections.emptyList();

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/battery_signal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signals)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要添加的电池信号数据")));

        // Verify service method was not called
        verify(batterySignalService, never()).save(any(BatterySignal.class));
        verify(batterySignalService, never()).saveBatch(anyList());
    }

    @Test
    void testUpdateSingleSignal() throws Exception {
        // Prepare test data
        BatterySignal signal = new BatterySignal();
        signal.setId(1L);
        signal.setCarId(101);
        signal.setWarnId(201);
        signal.setSignalData("{\"temperature\": 70, \"voltage\": 3.7}");
        signal.setProcessed(true);
        List<BatterySignal> signals = Collections.singletonList(signal);

        // Mock service response
        when(batterySignalService.updateById(any(BatterySignal.class))).thenReturn(true);

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/battery_signal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signals)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(batterySignalService, times(1)).updateById(any(BatterySignal.class));
    }

    @Test
    void testUpdateMultipleSignals() throws Exception {
        // Prepare test data
        BatterySignal signal1 = new BatterySignal();
        signal1.setId(1L);
        signal1.setCarId(101);
        signal1.setWarnId(201);
        signal1.setProcessed(true);

        BatterySignal signal2 = new BatterySignal();
        signal2.setId(2L);
        signal2.setCarId(102);
        signal2.setWarnId(202);
        signal2.setProcessed(true);

        List<BatterySignal> signals = Arrays.asList(signal1, signal2);

        // Mock service response
        when(batterySignalService.saveBatch(anyList())).thenReturn(true);

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/battery_signal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signals)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(batterySignalService, times(1)).saveBatch(anyList());
    }

    @Test
    void testUpdateEmptyList() throws Exception {
        // Prepare test data
        List<BatterySignal> signals = Collections.emptyList();

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/battery_signal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signals)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要更新的电池信号数据")));

        // Verify service method was not called
        verify(batterySignalService, never()).updateById(any(BatterySignal.class));
        verify(batterySignalService, never()).saveBatch(anyList());
    }

    @Test
    void testDeleteSingleSignal() throws Exception {
        // Mock service response
        when(batterySignalService.removeById(anyLong())).thenReturn(true);

        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/battery_signal")
                .param("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(batterySignalService, times(1)).removeById(eq(1L));
    }

    @Test
    void testDeleteMultipleSignals() throws Exception {
        // Mock service response
        when(batterySignalService.removeBatchByIds(anyList())).thenReturn(true);

        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/battery_signal")
                .param("ids", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(batterySignalService, times(1)).removeBatchByIds(eq(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    void testDeleteEmptyIds() throws Exception {
        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/battery_signal")
                .param("ids", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要删除的电池信号ID")));

        // Verify service method was not called
        verify(batterySignalService, never()).removeById(anyLong());
        verify(batterySignalService, never()).removeBatchByIds(anyList());
    }
} 