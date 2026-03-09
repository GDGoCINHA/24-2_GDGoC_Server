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

    @PostMapping("/scores")
    public ResponseEntity<ApiResponse<Void, Void>> submitScore(
            @Valid @RequestBody Rythm8beatScoreRequest request) {
        rythm8beatScoreService.submitScore(request);
        return ResponseEntity.ok(ApiResponse.ok(SCORE_SUBMITTED));
    }

    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<Rythm8beatRankingResponse, Void>> getRanking(
            @RequestParam(required = false) String phoneNumber) {
        Rythm8beatRankingResponse response = rythm8beatScoreService.getRanking(phoneNumber);
        return ResponseEntity.ok(ApiResponse.ok(RANKING_RETRIEVED, response));
    }

    @DeleteMapping("/scores/all")
    public ResponseEntity<ApiResponse<Void, Void>> resetAll() {
        rythm8beatScoreService.resetAll();
        return ResponseEntity.ok(ApiResponse.ok(SCORES_RESET));
    }
}
