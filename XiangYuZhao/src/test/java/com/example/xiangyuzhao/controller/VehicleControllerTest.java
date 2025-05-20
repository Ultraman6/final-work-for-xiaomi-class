package com.example.xiangyuzhao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.xiangyuzhao.dto.resp.BaseResponse;
import com.example.xiangyuzhao.entity.Vehicle;
import com.example.xiangyuzhao.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VehicleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private VehicleController vehicleController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(vehicleController).build();
    }

    @Test
    void testQuery() throws Exception {
        // Prepare test data
        List<Vehicle> vehicleList = new ArrayList<>();
        Vehicle vehicle = new Vehicle();
        vehicle.setCarId(1);
        vehicle.setVid("TEST001");
        vehicle.setBatteryType("LithiumIon");
        vehicle.setMileage(BigDecimal.valueOf(10000));
        vehicle.setBatteryHealth(BigDecimal.valueOf(95.5));
        vehicleList.add(vehicle);

        IPage<Vehicle> page = new Page<>(1, 10);
        page.setRecords(vehicleList);
        page.setTotal(1);

        // Mock service response
        when(vehicleService.page(eq(1), eq(10), any(QueryWrapper.class))).thenReturn(page);

        // Perform the GET request and validate response
        mockMvc.perform(get("/api/vehicle")
                .param("page", "1")
                .param("size", "10")
                .param("vid", "TEST001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].carId", is(1)))
                .andExpect(jsonPath("$.data.records[0].vid", is("TEST001")))
                .andExpect(jsonPath("$.data.total", is(1)))
                .andExpect(jsonPath("$.data.size", is(10)))
                .andExpect(jsonPath("$.data.current", is(1)));

        // Verify service method was called
        verify(vehicleService, times(1)).page(eq(1), eq(10), any(QueryWrapper.class));
    }

    @Test
    void testInsertSingleVehicle() throws Exception {
        // Prepare test data
        Vehicle vehicle = new Vehicle();
        vehicle.setVid("TEST001");
        vehicle.setBatteryType("LithiumIon");
        vehicle.setMileage(BigDecimal.valueOf(10000));
        vehicle.setBatteryHealth(BigDecimal.valueOf(95.5));
        List<Vehicle> vehicles = Collections.singletonList(vehicle);

        // Mock service response
        doNothing().when(vehicleService).save(any(Vehicle.class));

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/vehicle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vehicles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].vid", is("TEST001")));

        // Verify service method was called
        verify(vehicleService, times(1)).save(any(Vehicle.class));
    }

    @Test
    void testInsertMultipleVehicles() throws Exception {
        // Prepare test data
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setVid("TEST001");
        vehicle1.setBatteryType("LithiumIon");
        vehicle1.setMileage(BigDecimal.valueOf(10000));
        vehicle1.setBatteryHealth(BigDecimal.valueOf(95.5));

        Vehicle vehicle2 = new Vehicle();
        vehicle2.setVid("TEST002");
        vehicle2.setBatteryType("LithiumIon");
        vehicle2.setMileage(BigDecimal.valueOf(15000));
        vehicle2.setBatteryHealth(BigDecimal.valueOf(92.0));

        List<Vehicle> vehicles = Arrays.asList(vehicle1, vehicle2);

        // Mock service response
        doNothing().when(vehicleService).saveBatch(anyList());

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/vehicle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vehicles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].vid", is("TEST001")))
                .andExpect(jsonPath("$.data[1].vid", is("TEST002")));

        // Verify service method was called
        verify(vehicleService, times(1)).saveBatch(anyList());
    }

    @Test
    void testInsertEmptyList() throws Exception {
        // Prepare test data
        List<Vehicle> vehicles = Collections.emptyList();

        // Perform the POST request and validate response
        mockMvc.perform(post("/api/vehicle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vehicles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要添加的车辆数据")));

        // Verify service method was not called
        verify(vehicleService, never()).save(any(Vehicle.class));
        verify(vehicleService, never()).saveBatch(anyList());
    }

    @Test
    void testUpdateSingleVehicle() throws Exception {
        // Prepare test data
        Vehicle vehicle = new Vehicle();
        vehicle.setCarId(1);
        vehicle.setVid("TEST001");
        vehicle.setBatteryType("LithiumIon-Updated");
        vehicle.setMileage(BigDecimal.valueOf(10500));
        vehicle.setBatteryHealth(BigDecimal.valueOf(94.5));
        List<Vehicle> vehicles = Collections.singletonList(vehicle);

        // Mock service response
        when(vehicleService.updateById(any(Vehicle.class))).thenReturn(true);

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/vehicle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vehicles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(vehicleService, times(1)).updateById(any(Vehicle.class));
    }

    @Test
    void testUpdateMultipleVehicles() throws Exception {
        // Prepare test data
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setCarId(1);
        vehicle1.setVid("TEST001");
        vehicle1.setBatteryType("LithiumIon-Updated");

        Vehicle vehicle2 = new Vehicle();
        vehicle2.setCarId(2);
        vehicle2.setVid("TEST002");
        vehicle2.setBatteryType("LithiumIon-Updated");

        List<Vehicle> vehicles = Arrays.asList(vehicle1, vehicle2);

        // Mock service response
        when(vehicleService.saveBatch(anyList())).thenReturn(true);

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/vehicle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vehicles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(vehicleService, times(1)).saveBatch(anyList());
    }

    @Test
    void testUpdateEmptyList() throws Exception {
        // Prepare test data
        List<Vehicle> vehicles = Collections.emptyList();

        // Perform the PUT request and validate response
        mockMvc.perform(put("/api/vehicle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vehicles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要更新的车辆数据")));

        // Verify service method was not called
        verify(vehicleService, never()).updateById(any(Vehicle.class));
        verify(vehicleService, never()).saveBatch(anyList());
    }

    @Test
    void testDeleteSingleVehicle() throws Exception {
        // Mock service response
        when(vehicleService.removeById(anyInt())).thenReturn(true);

        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/vehicle")
                .param("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(vehicleService, times(1)).removeById(eq(1));
    }

    @Test
    void testDeleteMultipleVehicles() throws Exception {
        // Mock service response
        when(vehicleService.removeBatchByIds(anyList())).thenReturn(true);

        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/vehicle")
                .param("ids", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify service method was called
        verify(vehicleService, times(1)).removeBatchByIds(eq(Arrays.asList(1, 2, 3)));
    }

    @Test
    void testDeleteEmptyIds() throws Exception {
        // Perform the DELETE request and validate response
        mockMvc.perform(delete("/api/vehicle")
                .param("ids", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("没有要删除的车辆ID")));

        // Verify service method was not called
        verify(vehicleService, never()).removeById(anyInt());
        verify(vehicleService, never()).removeBatchByIds(anyList());
    }
} 