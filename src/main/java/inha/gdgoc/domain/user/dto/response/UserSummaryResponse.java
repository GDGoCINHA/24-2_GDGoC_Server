package inha.gdgoc.domain.user.dto.response;

import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;

public record UserSummaryResponse(
        Long id,
        String name,
        String major,
        String studentId,
        String email,
        UserRole userRole,
        TeamType team
) {}