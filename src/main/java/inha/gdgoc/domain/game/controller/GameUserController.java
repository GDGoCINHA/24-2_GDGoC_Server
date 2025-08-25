package inha.gdgoc.domain.game.controller;

import inha.gdgoc.domain.game.dto.request.GameUserRequest;
import inha.gdgoc.domain.game.dto.response.GameUserResponse;
import inha.gdgoc.domain.game.service.GameUserService;
import inha.gdgoc.global.dto.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class GameUserController {

    private final GameUserService gameUserService;

    @PostMapping("/game/result")
    public ResponseEntity<ApiResponse<List<GameUserResponse>>> saveGameResult(@RequestBody GameUserRequest request) {
        return ResponseEntity.ok(ApiResponse.of(gameUserService.saveGameResultAndGetRanking(request)));
    }

    @GetMapping("/game/results")
    public ResponseEntity<ApiResponse<List<GameUserResponse>>> getUserRankings() {
        return ResponseEntity.ok(ApiResponse.of(gameUserService.findUserRankings()));
    }
}
