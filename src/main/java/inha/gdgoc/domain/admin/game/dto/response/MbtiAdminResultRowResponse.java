package inha.gdgoc.domain.admin.game.dto.response;

import inha.gdgoc.domain.game.entity.MbtiResult;
import java.time.Instant;

public record MbtiAdminResultRowResponse(
        Long id,
        String name,
        String studentId,
        String mbtiType,
        Instant updatedAt,
        Instant createdAt
) {
    public static MbtiAdminResultRowResponse from(MbtiResult entity) {
        return new MbtiAdminResultRowResponse(
                entity.getId(),
                entity.getName(),
                entity.getStudentId(),
                entity.getMbtiType(),
                entity.getUpdatedAt(),
                entity.getCreatedAt()
        );
    }
}
