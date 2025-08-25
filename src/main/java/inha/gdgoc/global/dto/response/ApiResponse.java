package inha.gdgoc.global.dto.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final T data;
    private final Object meta; // meta는 optional

    private ApiResponse(T data, Object meta) {
        this.data = data;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> of(T data, Object meta) {
        return new ApiResponse<>(data, meta);
    }
}
