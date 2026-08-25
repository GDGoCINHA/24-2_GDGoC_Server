package inha.gdgoc.domain.admin.landing.controller;

import static inha.gdgoc.domain.landing.controller.message.LandingMessage.LANDING_DRAFT_RETRIEVED;
import static inha.gdgoc.domain.landing.controller.message.LandingMessage.LANDING_DRAFT_SAVED;
import static inha.gdgoc.domain.landing.controller.message.LandingMessage.LANDING_PUBLISHED;

import inha.gdgoc.domain.landing.dto.LandingContentPayload;
import inha.gdgoc.domain.landing.service.LandingContentService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 온보딩 콘텐츠 관리.
 *
 * <p>공개 첫 화면을 바꾸는 일이라 게시판 글쓰기(CORE)보다 좁게 잡는다. 클래스에 건 조건이 모든 메서드에
 * 걸리므로 조회도 함께 막힌다 — 편집 중인 초안이 밖으로 새지 않게 하려는 것이다.
 */
@RestController
@RequestMapping("/api/v1/admin/landing-content")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.LEAD))
public class LandingAdminController {

    private final LandingContentService landingContentService;

    /** 편집 중인 초안. 초안이 없으면 발행본을, 둘 다 없으면 빈 값을 준다. */
    @GetMapping
    public ResponseEntity<ApiResponse<LandingContentPayload, Void>> getDraft() {
        return ResponseEntity.ok(
            ApiResponse.ok(LANDING_DRAFT_RETRIEVED, landingContentService.findDraft().orElse(null)));
    }

    /** 초안을 통째로 덮어쓴다. 부분 수정은 없다 — 화면이 문서 전체를 들고 있다가 한 번에 보낸다. */
    @PutMapping
    public ResponseEntity<ApiResponse<Void, Void>> saveDraft(
        @AuthenticationPrincipal CustomUserDetails me,
        @Valid @RequestBody LandingContentPayload payload) {
        landingContentService.saveDraft(payload, me.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(LANDING_DRAFT_SAVED));
    }

    /** 초안을 방문자에게 내보낸다. */
    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<Void, Void>> publish(
        @AuthenticationPrincipal CustomUserDetails me) {
        landingContentService.publish(me.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(LANDING_PUBLISHED));
    }
}
