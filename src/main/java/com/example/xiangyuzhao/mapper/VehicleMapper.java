package com.example.xiangyuzhao.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.xiangyuzhao.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {
    /**
     * Find vehicle by VID
     * @param vid Vehicle ID (16-character string)
     * @return Vehicle entity
     */
    Vehicle findByVid(@Param("vid") String vid);
    
    /**
     * Find vehicle by car ID
     * @param carId Car ID
     * @return Vehicle entity
     */
    Vehicle findByCarId(@Param("carId") Integer carId);
    
    /**
     * Update vehicle battery health
     * @param carId Car ID
     * @param batteryHealth Battery health percentage
     * @return Number of rows affected
     */
    int updateBatteryHealth(@Param("carId") Integer carId, @Param("batteryHealth") BigDecimal batteryHealth);
    
    /**
     * Update vehicle mileage
     * @param carId Car ID
     * @param mileage Total mileage in km
     * @return Number of rows affected
     */
    int updateMileage(@Param("carId") Integer carId, @Param("mileage") BigDecimal mileage);
    
    /**
     * Find all vehicles by battery type
     * @param batteryType Battery type
     * @return List of vehicles
     */
    List<Vehicle> findByBatteryType(@Param("batteryType") String batteryType);
    
    /**
     * Find vehicles with pagination
     * @param page Pagination object
     * @return Page of vehicle data
     */
    IPage<Vehicle> selectVehiclePage(IPage<Vehicle> page);
} 