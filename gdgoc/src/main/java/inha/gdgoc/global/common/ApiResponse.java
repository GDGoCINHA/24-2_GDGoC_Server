package inha.gdgoc.global.common;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private boolean isSuccess;       // 성공 여부
    private String message;        // 응답 메시지
    private T data;                // 실제 데이터
    private int statusCode;

    private ApiResponse(boolean isSuccess, String message, T data, int statusCode) {
        this.isSuccess = isSuccess;
        this.message = message;
        this.data = data;
        this.statusCode = statusCode;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", data, HttpStatus.OK.value());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, HttpStatus.OK.value());
    }

    public static <T> ApiResponse<T> success(String message, T data, HttpStatus status) {
        return new ApiResponse<>(true, message, data, status.value());
    }

    public static ApiResponse<?> failure(String message, HttpStatus status) {
        return new ApiResponse<>(false, message, null, status.value());
    }
}