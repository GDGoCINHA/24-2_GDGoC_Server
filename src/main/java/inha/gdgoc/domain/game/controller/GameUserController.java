package inha.gdgoc.domain.game.controller;

import static inha.gdgoc.domain.game.controller.message.GameUserMessage.GAME_RANK_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.game.controller.message.GameUserMessage.GAME_RANK_SAVE_SUCCESS;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
@RestController
public class GameUserController {

    private final GameUserService gameUserService;

    @PostMapping("/result")
    public ResponseEntity<ApiResponse<List<GameUserResponse>, Void>> saveGameResult(
            @RequestBody GameUserRequest request
    ) {
        List<GameUserResponse> response = gameUserService.saveGameResultAndGetRanking(request);

        return ResponseEntity.ok(ApiResponse.ok(GAME_RANK_SAVE_SUCCESS, response));
    }

    @GetMapping("/results")
    public ResponseEntity<ApiResponse<List<GameUserResponse>, Void>> getUserRankings() {
        List<GameUserResponse> response = gameUserService.findUserRankings();

        return ResponseEntity.ok(ApiResponse.ok(GAME_RANK_RETRIEVED_SUCCESS, response));
    }
}
