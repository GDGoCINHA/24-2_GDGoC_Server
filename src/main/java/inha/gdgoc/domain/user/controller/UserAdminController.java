package inha.gdgoc.domain.user.controller;

import inha.gdgoc.domain.user.dto.request.UpdateRoleRequest;
import inha.gdgoc.domain.user.dto.request.UpdateUserRoleTeamRequest;
import inha.gdgoc.domain.user.dto.response.UserSummaryResponse;
import inha.gdgoc.domain.user.service.UserAdminService;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @Operation(summary = "사용자 요약 목록 조회", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize("hasAnyRole('LEAD','ORGANIZER','ADMIN') or T(inha.gdgoc.domain.user.enums.TeamType).HR == principal.team")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>, PageMeta>> list(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "name") String sort, @RequestParam(defaultValue = "ASC") String dir) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<UserSummaryResponse> result = userAdminService.listUsers(q, pageable);
        return ResponseEntity.ok(ApiResponse.ok("USER_SUMMARY_LIST_RETRIEVED", result, PageMeta.of(result)));
    }

    @Operation(summary = "사용자 역할/팀 수정", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize("hasAnyRole('LEAD','ORGANIZER','ADMIN')")
    @PatchMapping("/{userId}/role-team")
    public ResponseEntity<ApiResponse<Void, Void>> updateRoleTeam(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long userId, @RequestBody UpdateUserRoleTeamRequest req) {
        userAdminService.updateRoleAndTeam(me, userId, req);
        return ResponseEntity.ok(ApiResponse.ok("USER_ROLE_TEAM_UPDATED"));
    }

    @Operation(summary = "사용자 역할 수정", security = {@SecurityRequirement(name = "BearerAuth")})
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasAnyRole('LEAD','ORGANIZER','ADMIN') or T(inha.gdgoc.domain.user.enums.TeamType).HR == principal.team")
    public ResponseEntity<ApiResponse<Void, Void>> updateUserRole(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long userId, @RequestBody @Valid UpdateRoleRequest req) {
        userAdminService.updateUserRoleWithRules(me, userId, req.role());
        return ResponseEntity.ok(ApiResponse.ok("USER_ROLE_UPDATED"));
    }

    @Operation(summary = "사용자 삭제", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize("hasAnyRole('LEAD', 'ORGANIZER', 'ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void, Void>> deleteUser(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long userId) {
        userAdminService.deleteUserWithRules(me, userId);
        return ResponseEntity.ok(ApiResponse.ok("USER_DELETED"));
    }
}