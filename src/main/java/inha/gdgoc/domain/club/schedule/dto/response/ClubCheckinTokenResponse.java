package inha.gdgoc.domain.club.schedule.dto.response;

/**
 * 리더 화면이 QR 로 그릴 값. {@code expiresInSeconds} 가 지나기 전에 다시 받아 QR 을 새로 그린다.
 */
public record ClubCheckinTokenResponse(Long scheduleId, String token, long expiresInSeconds) {}
