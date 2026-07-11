package org.firststep.backend.shared.dto;

import java.time.Instant;

public class ApiResponse<T> {
    public boolean success;
    public T data;
    public String errorCode;
    public String errorMessage;
    public String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.timestamp = Instant.now().toString();
        return response;
    }

    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.errorCode = errorCode;
        response.errorMessage = errorMessage;
        response.timestamp = Instant.now().toString();
        return response;
    }
}
