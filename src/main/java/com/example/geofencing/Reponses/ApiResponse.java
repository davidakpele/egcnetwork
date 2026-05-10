package com.example.geofencing.Reponses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
 
@Data 
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
    private LocalDateTime timestamp;
 
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true).message(message).data(data)
            .timestamp(LocalDateTime.now()).build();
    }
 
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation successful");
    }
 
    public static <T> ApiResponse<T> error(String error, String message) {
        return ApiResponse.<T>builder()
            .success(false).error(error).message(message)
            .timestamp(LocalDateTime.now()).build();
    }
}