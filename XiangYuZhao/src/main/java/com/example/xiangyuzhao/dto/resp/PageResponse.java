package com.example.xiangyuzhao.dto.resp;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分页响应类
 * @param <T> 分页数据类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResponse<T> extends BaseResponse<List<T>> {
    /**
     * 当前页
     */
    private long current;
    
    /**
     * 每页大小
     */
    private long size;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 总页数
     */
    private long pages;
    
    /**
     * 创建分页响应
     * @param records 分页数据
     * @param current 当前页
     * @param size 每页大小
     * @param total 总记录数
     * @return 分页响应对象
     */
    public static <T> PageResponse<T> success(List<T> records, long current, long size, long total) {
        PageResponse<T> response = new PageResponse<>();
        response.setStatus(200);
        response.setMsg("ok");
        response.setData(records);
        response.setCurrent(current);
        response.setSize(size);
        response.setTotal(total);
        response.setPages(total % size == 0 ? total / size : total / size + 1);
        return response;
    }
} 