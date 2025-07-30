package com.jinx.statistics.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class ApiResponse<T> implements Serializable {

    /**
     * 结果代码：1成功，0和其它数字为失败
     */
    private Integer code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 无数据成功响应
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> success(){
        ApiResponse<T> response = new ApiResponse<>();
        response.code = 1;
        return response;
    }

    /**
     * 有数据成功响应
     * @param data
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> success(T data){
        ApiResponse<T> response = new ApiResponse<>();
        response.code = 1;
        response.data = data;
        return response;
    }

    /**
     * 无数据失败响应
     * @param message
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> error(String message){
        ApiResponse<T> response = new ApiResponse<>();
        response.code = 0;
        response.message = message;
        return response;
    }
}
