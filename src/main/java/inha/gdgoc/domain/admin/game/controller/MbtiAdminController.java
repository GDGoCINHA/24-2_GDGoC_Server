package inha.gdgoc.domain.admin.game.controller;

import inha.gdgoc.domain.admin.game.dto.request.MbtiTeamMatchRequest;
import inha.gdgoc.domain.admin.game.dto.response.MbtiAdminResultRowResponse;
import inha.gdgoc.domain.admin.game.dto.response.MbtiTeamMatchResponse;
import inha.gdgoc.domain.admin.game.service.MbtiAdminService;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/game/mbti")
public class MbtiAdminController {

    private static final String CORE_OR_HIGHER_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).CORE))";

    private final MbtiAdminService mbtiAdminService;

    @PreAuthorize(CORE_OR_HIGHER_RULE)
    @GetMapping("/results")
    public ResponseEntity<ApiResponse<Page<MbtiAdminResultRowResponse>, PageMeta>> listResults(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "DESC") String dir
    ) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        Page<MbtiAdminResultRowResponse> result = mbtiAdminService.searchResults(q, pageable);

        return ResponseEntity.ok(ApiResponse.ok("MBTI_RESULT_LIST_RETRIEVED", result, PageMeta.of(result)));
    }

    @PreAuthorize(CORE_OR_HIGHER_RULE)
    @PostMapping("/team-matching")
    public ResponseEntity<ApiResponse<MbtiTeamMatchResponse, Void>> matchTeams(
            @Valid @RequestBody MbtiTeamMatchRequest request
    ) {
        MbtiTeamMatchResponse response = mbtiAdminService.matchTeams(request);
        return ResponseEntity.ok(ApiResponse.ok("MBTI_TEAM_MATCHING_COMPLETED", response));
    }
}
