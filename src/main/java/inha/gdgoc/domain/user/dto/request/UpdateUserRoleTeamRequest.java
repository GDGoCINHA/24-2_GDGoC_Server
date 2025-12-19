package inha.gdgoc.domain.user.dto.request;

import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;

public record UpdateUserRoleTeamRequest(
        UserRole role,   // null 이면 변경 안 함
        TeamType team    // null 이면 변경 안 함
) {}