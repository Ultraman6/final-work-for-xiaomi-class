package com.example.xiangyuzhao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.entity.WarnRule;
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
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WarnRuleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WarnRuleService warnRuleService;

    @InjectMocks
    private WarnRuleController warnRuleController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(warnRuleController).build();
    }

    @Test
    void testQuery() throws Exception {
        // Prepare test data
        List<WarnRule> warnRuleList = new ArrayList<>();
        WarnRule warnRule = new WarnRule();
        warnRule.setId(1L);
        warnRule.setWarnId(101);
        warnRule.setWarnName("电池温度过高");
        warnRule.setBatteryType("LithiumIon");
        warnRule.setRule("temperature > 60");
        warnRuleList.add(warnRule);

        IPage<WarnRule> page = new Page<>(1, 10);
        page.setRecords(warnRuleList);
        page.setTotal(1);

        // Mock service response
        when(warnRuleService.page(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

        // Perform the GET request and validate response
        mockMvc.perform(get("/api/warn_rule")
                .param("page", "1")
                .param("size", "10")
                .param("warnId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].id", is(1)))
                .andExpect(jsonPath("$.data.records[0].warnId", is(101)))
                .andExpect(jsonPath("$.data.records[0].warnName", is("电池温度过高")))
                .andExpect(jsonPath("$.data.total", is(1)))
                .andExpect(jsonPath("$.data.size", is(10)))
                .andExpect(jsonPath("$.data.current", is(1)));

        // Verify service method was called
        verify(warnRuleService, times(1)).page(any(Page.class), any(QueryWrapper.class));
    }

    @Test
    void testInsertSingleWarnRule() throws Exception {
        // Prepare test data
        WarnRule warnRule = new WarnRule();
        warnRule.setWarnId(101);
        warnRule.setWarnName("电池温度过高");
        warnRule.setBatteryType("LithiumIon");
        warnRule.setRule("temperature > 60");
        List<WarnRule> warnRules = Collections.singletonList(warnRule);

        // Mock service response
        doNothing().when(warnRuleService).save(any(WarnRule.class));

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/warn_rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnRules)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].warnId", is(101)))
                .andExpect(jsonPath("$.data[0].warnName", is("电池温度过高")));

        // Verify service method was called
        verify(warnRuleService, times(1)).save(any(WarnRule.class));
    }

    @Test
    void testInsertMultipleWarnRules() throws Exception {
        // Prepare test data
        WarnRule warnRule1 = new WarnRule();
        warnRule1.setWarnId(101);
        warnRule1.setWarnName("电池温度过高");
        warnRule1.setBatteryType("LithiumIon");
        warnRule1.setRule("temperature > 60");

        WarnRule warnRule2 = new WarnRule();
        warnRule2.setWarnId(102);
        warnRule2.setWarnName("电池电压过低");
        warnRule2.setBatteryType("LithiumIon");
        warnRule2.setRule("voltage < 3.2");

        List<WarnRule> warnRules = Arrays.asList(warnRule1, warnRule2);

        // Mock service response
        doNothing().when(warnRuleService).saveBatch(anyList());

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/warn_rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnRules)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].warnId", is(101)))
                .andExpect(jsonPath("$.data[1].warnId", is(102)));

        // Verify service method was called
        verify(warnRuleService, times(1)).saveBatch(anyList());
    }

    @Test
    void testInsertEmptyList() throws Exception {
        // Prepare test data
        List<WarnRule> warnRules = Collections.emptyList();

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/warn_rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnRules)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要添加的预警规则数据")));

        // Verify service method was not called
        verify(warnRuleService, never()).save(any(WarnRule.class));
        verify(warnRuleService, never()).saveBatch(anyList());
    }

    @Test
    void testUpdateSingleWarnRule() throws Exception {
        // Prepare test data
        WarnRule warnRule = new WarnRule();
        warnRule.setId(1L);
        warnRule.setWarnId(101);
        warnRule.setWarnName("电池温度过高-更新");
        warnRule.setBatteryType("LithiumIon");
        warnRule.setRule("temperature > 65");
        List<WarnRule> warnRules = Collections.singletonList(warnRule);

        // Mock service response
        when(warnRuleService.updateById(any(WarnRule.class))).thenReturn(true);

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/warn_rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnRules)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(warnRuleService, times(1)).updateById(any(WarnRule.class));
    }

    @Test
    void testUpdateMultipleWarnRules() throws Exception {
        // Prepare test data
        WarnRule warnRule1 = new WarnRule();
        warnRule1.setId(1L);
        warnRule1.setWarnId(101);
        warnRule1.setWarnName("电池温度过高-更新");

        WarnRule warnRule2 = new WarnRule();
        warnRule2.setId(2L);
        warnRule2.setWarnId(102);
        warnRule2.setWarnName("电池电压过低-更新");

        List<WarnRule> warnRules = Arrays.asList(warnRule1, warnRule2);

        // Mock service response
        when(warnRuleService.saveBatch(anyList())).thenReturn(true);

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/warn_rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnRules)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(warnRuleService, times(1)).saveBatch(anyList());
    }

    @Test
    void testUpdateEmptyList() throws Exception {
        // Prepare test data
        List<WarnRule> warnRules = Collections.emptyList();

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/warn_rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnRules)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要更新的预警规则数据")));

        // Verify service method was not called
        verify(warnRuleService, never()).updateById(any(WarnRule.class));
        verify(warnRuleService, never()).saveBatch(anyList());
    }

    @Test
    void testDeleteSingleWarnRule() throws Exception {
        // Mock service response
        when(warnRuleService.removeById(anyLong())).thenReturn(true);

        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/warn_rule")
                .param("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(warnRuleService, times(1)).removeById(eq(1L));
    }

    @Test
    void testDeleteMultipleWarnRules() throws Exception {
        // Mock service response
        when(warnRuleService.removeByIds(anyList())).thenReturn(true);

        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/warn_rule")
                .param("ids", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(warnRuleService, times(1)).removeByIds(eq(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    void testDeleteEmptyIds() throws Exception {
        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/warn_rule")
                .param("ids", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要删除的预警规则ID")));

        // Verify service method was not called
        verify(warnRuleService, never()).removeById(anyLong());
        verify(warnRuleService, never()).removeByIds(anyList());
    }
} 