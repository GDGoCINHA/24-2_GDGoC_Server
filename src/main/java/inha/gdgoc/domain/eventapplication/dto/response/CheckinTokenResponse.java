package inha.gdgoc.domain.eventapplication.dto.response;

/**
 * 관리자 화면이 QR 로 그릴 값.
 *
 * <p>{@code expiresInSeconds} 가 0 이 되기 전에 다시 받아 QR 을 새로 그린다.
 */
public record CheckinTokenResponse(Long eventBoardId, String token, long expiresInSeconds) {}
