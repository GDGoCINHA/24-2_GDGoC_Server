package inha.gdgoc.domain.game.controller;

import static inha.gdgoc.domain.game.controller.message.Rythm8beatScoreMessage.RANKING_RETRIEVED;
import static inha.gdgoc.domain.game.controller.message.Rythm8beatScoreMessage.SCORES_RESET;
import static inha.gdgoc.domain.game.controller.message.Rythm8beatScoreMessage.SCORE_SUBMITTED;

import inha.gdgoc.domain.game.dto.request.Rythm8beatScoreRequest;
import inha.gdgoc.domain.game.dto.response.Rythm8beatRankingResponse;
import inha.gdgoc.domain.game.service.Rythm8beatScoreService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/game/rythm8beat")
@RestController
@RequiredArgsConstructor
public class Rythm8beatScoreController {

    private final Rythm8beatScoreService rythm8beatScoreService;

    /**
     * 리듬8비트 점수 제출 요청을 처리한다.
     *
     * @param request 제출할 점수 정보를 담은 요청 본문(유효성 검증됨)
     * @return 성공 처리 결과를 담은 ApiResponse. 페이로드는 비어있고 메시지는 SCORE_SUBMITTED
     */
    @PostMapping("/scores")
    public ResponseEntity<ApiResponse<Void, Void>> submitScore(
            @Valid @RequestBody Rythm8beatScoreRequest request) {
        rythm8beatScoreService.submitScore(request);
        return ResponseEntity.ok(ApiResponse.ok(SCORE_SUBMITTED));
    }

    /**
     * 리듬8비트 게임의 전체 랭킹 및 (선택적으로) 특정 사용자 랭킹을 조회합니다.
     *
     * @param phoneNumber 조회할 사용자의 전화번호(선택). 제공하면 해당 사용자의 순위 정보가 응답에 포함됩니다.
     * @return 랭킹 정보를 담은 Rythm8beatRankingResponse를 페이로드로 포함하는 ApiResponse를 담은 ResponseEntity
     */
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<Rythm8beatRankingResponse, Void>> getRanking(
            @RequestParam(required = false) String phoneNumber) {
        Rythm8beatRankingResponse response = rythm8beatScoreService.getRanking(phoneNumber);
        return ResponseEntity.ok(ApiResponse.ok(RANKING_RETRIEVED, response));
    }

    /**
     * 모든 리듬8비트 점수 데이터를 초기화합니다.
     *
     * @return ApiResponse에 빈 페이로드와 SCORES_RESET 메시지를 포함한 HTTP 200 OK 응답
     */
    @DeleteMapping("/scores/all")
    public ResponseEntity<ApiResponse<Void, Void>> resetAll() {
        rythm8beatScoreService.resetAll();
        return ResponseEntity.ok(ApiResponse.ok(SCORES_RESET));
    }
}
