package com.example.xiangyuzhao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.entity.WarnInfo;
import com.example.xiangyuzhao.service.WarnInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WarnInfoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WarnInfoService warnInfoService;

    @InjectMocks
    private WarnInfoController warnInfoController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(warnInfoController).build();
    }

    @Test
    void testQuery() throws Exception {
        // Prepare test data
        List<WarnInfo> warnInfoList = new ArrayList<>();
        WarnInfo warnInfo = new WarnInfo();
        warnInfo.setId(1L);
        warnInfo.setCarId(101);
        warnInfo.setWarnId(201);
        warnInfo.setWarnName("电池温度过高");
        warnInfo.setWarnLevel(1);
        warnInfo.setSignalId(301L);
        warnInfo.setSignalTime(new Date());
        warnInfo.setWarnTime(new Date());
        warnInfoList.add(warnInfo);

        IPage<WarnInfo> page = new Page<>(1, 10);
        page.setRecords(warnInfoList);
        page.setTotal(1);

        // Mock service response
        when(warnInfoService.page(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

        // Perform the GET request and validate response
        mockMvc.perform(get("/api/warn_info")
                .param("page", "1")
                .param("size", "10")
                .param("carId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].id", is(1)))
                .andExpect(jsonPath("$.data.records[0].carId", is(101)))
                .andExpect(jsonPath("$.data.records[0].warnId", is(201)))
                .andExpect(jsonPath("$.data.records[0].warnName", is("电池温度过高")))
                .andExpect(jsonPath("$.data.total", is(1)))
                .andExpect(jsonPath("$.data.size", is(10)))
                .andExpect(jsonPath("$.data.current", is(1)));

        // Verify service method was called
        verify(warnInfoService, times(1)).page(any(Page.class), any(QueryWrapper.class));
    }

    @Test
    void testInsertSingleWarnInfo() throws Exception {
        // Prepare test data
        WarnInfo warnInfo = new WarnInfo();
        warnInfo.setCarId(101);
        warnInfo.setWarnId(201);
        warnInfo.setWarnName("电池温度过高");
        warnInfo.setWarnLevel(1);
        warnInfo.setSignalId(301L);
        warnInfo.setSignalTime(new Date());
        List<WarnInfo> warnInfos = Collections.singletonList(warnInfo);

        // Mock service response
        doNothing().when(warnInfoService).save(any(WarnInfo.class));

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/warn_info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnInfos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].carId", is(101)))
                .andExpect(jsonPath("$.data[0].warnId", is(201)));

        // Verify service method was called
        verify(warnInfoService, times(1)).save(any(WarnInfo.class));
    }

    @Test
    void testInsertMultipleWarnInfos() throws Exception {
        // Prepare test data
        WarnInfo warnInfo1 = new WarnInfo();
        warnInfo1.setCarId(101);
        warnInfo1.setWarnId(201);
        warnInfo1.setWarnName("电池温度过高");
        warnInfo1.setWarnLevel(1);
        warnInfo1.setSignalId(301L);
        warnInfo1.setSignalTime(new Date());

        WarnInfo warnInfo2 = new WarnInfo();
        warnInfo2.setCarId(102);
        warnInfo2.setWarnId(202);
        warnInfo2.setWarnName("电池电压过低");
        warnInfo2.setWarnLevel(2);
        warnInfo2.setSignalId(302L);
        warnInfo2.setSignalTime(new Date());

        List<WarnInfo> warnInfos = Arrays.asList(warnInfo1, warnInfo2);

        // Mock service response
        doNothing().when(warnInfoService).saveBatch(anyList());

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/warn_info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnInfos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].carId", is(101)))
                .andExpect(jsonPath("$.data[1].carId", is(102)));

        // Verify service method was called
        verify(warnInfoService, times(1)).saveBatch(anyList());
    }

    @Test
    void testInsertEmptyList() throws Exception {
        // Prepare test data
        List<WarnInfo> warnInfos = Collections.emptyList();

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/warn_info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnInfos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要添加的预警信息数据")));

        // Verify service method was not called
        verify(warnInfoService, never()).save(any(WarnInfo.class));
        verify(warnInfoService, never()).saveBatch(anyList());
    }

    @Test
    void testUpdateSingleWarnInfo() throws Exception {
        // Prepare test data
        WarnInfo warnInfo = new WarnInfo();
        warnInfo.setId(1L);
        warnInfo.setCarId(101);
        warnInfo.setWarnId(201);
        List<WarnInfo> warnInfos = Collections.singletonList(warnInfo);

        // Mock service response
        when(warnInfoService.updateById(any(WarnInfo.class))).thenReturn(true);

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/warn_info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnInfos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(warnInfoService, times(1)).updateById(any(WarnInfo.class));
    }

    @Test
    void testUpdateMultipleWarnInfos() throws Exception {
        // Prepare test data
        WarnInfo warnInfo1 = new WarnInfo();
        warnInfo1.setId(1L);
        warnInfo1.setCarId(101);
        warnInfo1.setWarnId(201);

        WarnInfo warnInfo2 = new WarnInfo();
        warnInfo2.setId(2L);
        warnInfo2.setCarId(102);
        warnInfo2.setWarnId(202);

        List<WarnInfo> warnInfos = Arrays.asList(warnInfo1, warnInfo2);

        // Mock service response
        when(warnInfoService.saveBatch(anyList())).thenReturn(true);

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/warn_info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(warnInfos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(warnInfoService, times(1)).saveBatch(anyList());
    }

    @Test
    void testDeleteSingleWarnInfo() throws Exception {
        // Mock service response
        when(warnInfoService.removeById(anyLong())).thenReturn(true);

        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/warn_info")
                .param("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(warnInfoService, times(1)).removeById(eq(1L));
    }

    @Test
    void testDeleteMultipleWarnInfos() throws Exception {
        // Mock service response
        when(warnInfoService.removeByIds(anyList())).thenReturn(true);

        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/warn_info")
                .param("ids", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(warnInfoService, times(1)).removeByIds(eq(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    void testDeleteEmptyIds() throws Exception {
        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/warn_info")
                .param("ids", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要删除的预警信息ID")));

        // Verify service method was not called
        verify(warnInfoService, never()).removeById(anyLong());
        verify(warnInfoService, never()).removeByIds(anyList());
    }
} 