package inha.gdgoc.domain.game.controller;

import static inha.gdgoc.domain.game.controller.message.GameQuestionMessage.GAME_QUESTION_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.game.controller.message.GameQuestionMessage.GAME_QUESTION_SAVE_SUCCESS;

import inha.gdgoc.domain.game.dto.request.GameQuestionRequest;
import inha.gdgoc.domain.game.dto.response.GameQuestionResponse;
import inha.gdgoc.domain.game.service.GameQuestionService;
import inha.gdgoc.global.dto.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/game/questions")
@RequiredArgsConstructor
@RestController
public class GameQuestionController {

    private final GameQuestionService gameQuestionService;

    // 얘 api 엔드포인트 바뀜!
    @PostMapping
    public ResponseEntity<ApiResponse<Void, Void>> saveQuestion(
            @RequestBody GameQuestionRequest gameQuestionRequest
    ) {
        gameQuestionService.saveQuestion(gameQuestionRequest);

        return ResponseEntity.ok(ApiResponse.ok(GAME_QUESTION_SAVE_SUCCESS));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GameQuestionResponse>, Void>> getRandomGameQuestions() {
        List<GameQuestionResponse> response = gameQuestionService.getRandomQuestionsByLanguage();

        return ResponseEntity.ok(ApiResponse.ok(GAME_QUESTION_RETRIEVED_SUCCESS, response));
    }
}
