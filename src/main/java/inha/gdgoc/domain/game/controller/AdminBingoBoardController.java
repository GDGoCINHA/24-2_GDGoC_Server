package inha.gdgoc.domain.game.controller;

import static inha.gdgoc.domain.game.controller.message.GameUserMessage.BINGO_BOARD_UPDATED_SUCCESS;

import inha.gdgoc.domain.game.dto.request.BingoBoardUpdateRequest;
import inha.gdgoc.domain.game.dto.response.BingoBoardResponse;
import inha.gdgoc.domain.game.service.BingoBoardService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/admin/game/bingo")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminBingoBoardController {

    private final BingoBoardService bingoBoardService;
    private final AccessGuard accessGuard;

    @PutMapping("/boards/{teamNumber}")
    public ResponseEntity<ApiResponse<BingoBoardResponse, Void>> updateBoard(
            @AuthenticationPrincipal TokenProvider.CustomUserDetails me,
            @PathVariable Integer teamNumber,
            @RequestBody BingoBoardUpdateRequest request
    ) {
        log.info(
                "빙고 수정 권한 검사: username={}, role={}, team={}",
                me != null ? me.getUsername() : null,
                me != null ? me.getRole() : null,
                me != null ? me.getTeam() : null
        );
        accessGuard.require(me, AccessGuard.AccessCondition.atLeast(UserRole.CORE));

        BingoBoardResponse response = bingoBoardService.updateBoard(teamNumber, request);
        return ResponseEntity.ok(ApiResponse.ok(BINGO_BOARD_UPDATED_SUCCESS, response));
    }
}
