package com.example.xiangyuzhao.dto.resp;

import lombok.Data;

/**
 * 通用响应类
 * @param <T> 返回数据的类型
 */
@Data
public class BaseResponse<T> {
    /**
     * 状态码
     */
    private Integer status;
    
    /**
     * 响应消息
     */
    private String msg;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 成功响应
     * @param data 响应数据
     * @return 响应对象
     */
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setStatus(200);
        response.setMsg("ok");
        response.setData(data);
        return response;
    }
    
    /**
     * 失败响应
     * @param status 状态码
     * @param msg 错误消息
     * @return 响应对象
     */
    public static <T> BaseResponse<T> error(int status, String msg) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setStatus(status);
        response.setMsg(msg);
        return response;
    }
} 