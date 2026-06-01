package com.secondhand.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "成功", data);
    }
    public static Result success() {
        return new Result<>(200, "成功", null);
    }
    public static Result error(String msg) {
        return new Result<>(500, msg, null);
    }
}