package com.roger.auth_service.dto;

import lombok.Data;

@Data
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private Object details;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.setSuccess(true);
        res.setCode("OK");
        res.setMessage("Success");
        res.setData(data);
        return res;
    }

    public static ApiResponse<Void> error(String code, String message, Object details) {
        ApiResponse<Void> res = new ApiResponse<>();
        res.setSuccess(false);
        res.setCode(code);
        res.setMessage(message);
        res.setDetails(details);
        return res;
    }

}

