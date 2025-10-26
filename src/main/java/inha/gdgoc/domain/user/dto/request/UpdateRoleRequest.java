package inha.gdgoc.domain.user.dto.request;

import inha.gdgoc.domain.user.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull UserRole role
) {}