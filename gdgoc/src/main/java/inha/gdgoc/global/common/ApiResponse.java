package inha.gdgoc.global.common;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private boolean isSuccess;       // 성공 여부
    private int statusCode;
    private T data;                // 실제 데이터
    private String message;        // 응답 메시지

    private ApiResponse(boolean isSuccess, int statusCode, T data, String message) {
        this.isSuccess = isSuccess;
        this.statusCode = statusCode;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), data, "요청이 성공적으로 처리되었습니다.");
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), data, message);
    }

    public static <T> ApiResponse<T> success( HttpStatus status, T data, String message) {
        return new ApiResponse<>(true, status.value(), data, message);
    }

    public static <T> ApiResponse<T> failure(HttpStatus status, T data, String message) {
        return new ApiResponse<>(false, status.value(), data, message);
    }
}