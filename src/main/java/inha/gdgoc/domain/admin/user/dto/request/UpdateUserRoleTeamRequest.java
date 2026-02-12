package inha.gdgoc.domain.admin.user.dto.request;

import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;

public record UpdateUserRoleTeamRequest(
        UserRole role,
        TeamType team
) {}
