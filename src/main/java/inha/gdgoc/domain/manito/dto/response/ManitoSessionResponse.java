package inha.gdgoc.domain.manito.dto.response;

import inha.gdgoc.domain.manito.entity.ManitoSession;

import java.time.Instant;

public record ManitoSessionResponse(Long id, String code, String title, Instant createdAt) {

    public static ManitoSessionResponse from(ManitoSession session) {
        return new ManitoSessionResponse(session.getId(), session.getCode(), session.getTitle(), session.getCreatedAt());
    }
}