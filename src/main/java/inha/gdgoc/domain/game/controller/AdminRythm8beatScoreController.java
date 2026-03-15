package inha.gdgoc.domain.game.controller;

import static inha.gdgoc.domain.game.controller.message.Rythm8beatScoreMessage.ADMIN_SCORES_RETRIEVED;

import inha.gdgoc.domain.game.dto.response.Rythm8beatAdminScoreResponse;
import inha.gdgoc.domain.game.service.Rythm8beatScoreService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.AccessGuard;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/admin/game/rythm8beat")
@RestController
@RequiredArgsConstructor
public class AdminRythm8beatScoreController {

    private final Rythm8beatScoreService rythm8beatScoreService;
    private final AccessGuard accessGuard;

    @GetMapping("/scores")
    public ResponseEntity<ApiResponse<List<Rythm8beatAdminScoreResponse>, Void>> getAllScores(
            @AuthenticationPrincipal TokenProvider.CustomUserDetails me
    ) {
        accessGuard.require(me, AccessGuard.AccessCondition.atLeast(UserRole.CORE));
        return ResponseEntity.ok(ApiResponse.ok(
                ADMIN_SCORES_RETRIEVED,
                rythm8beatScoreService.getAllScores()
        ));
    }
}
