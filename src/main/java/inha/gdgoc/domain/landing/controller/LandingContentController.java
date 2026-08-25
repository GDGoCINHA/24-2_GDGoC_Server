package inha.gdgoc.domain.landing.controller;

import static inha.gdgoc.domain.landing.controller.message.LandingMessage.LANDING_CONTENT_RETRIEVED;

import inha.gdgoc.domain.landing.dto.LandingContentPayload;
import inha.gdgoc.domain.landing.service.LandingContentService;
import inha.gdgoc.global.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 방문자가 보는 온보딩 콘텐츠.
 *
 * <p>로그인 없이 여는 첫 화면이라 공개다. SecurityConfig 의 permitAll 에 이 경로가 있어야 한다.
 *
 * <p>발행된 게 없으면 {@code data} 가 비어 있다. 404 가 아니다 — 웹이 번들에 든 기본값을 쓰면 되고,
 * 아직 아무것도 발행하지 않은 상태는 오류가 아니다.
 */
@RestController
@RequestMapping("/api/v1/landing-content")
@RequiredArgsConstructor
public class LandingContentController {

    private final LandingContentService landingContentService;

    @GetMapping
    public ResponseEntity<ApiResponse<LandingContentPayload, Void>> getLandingContent() {
        return ResponseEntity.ok(
            ApiResponse.ok(
                LANDING_CONTENT_RETRIEVED, landingContentService.findPublished().orElse(null)));
    }
}
