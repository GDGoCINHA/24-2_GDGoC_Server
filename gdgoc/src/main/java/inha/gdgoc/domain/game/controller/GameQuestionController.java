package inha.gdgoc.domain.game.controller;

import inha.gdgoc.domain.game.dto.request.GameQuestionRequest;
import inha.gdgoc.domain.game.dto.response.GameQuestionResponse;
import inha.gdgoc.domain.game.entity.GameQuestion;
import inha.gdgoc.domain.game.service.GameQuestionService;
import inha.gdgoc.global.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class GameQuestionController {

    private final GameQuestionService gameQuestionService;

    @PostMapping("/game/question")
    public ResponseEntity<ApiResponse<GameQuestionRequest>> saveQuestion(
            @RequestBody GameQuestionRequest gameQuestionRequest) {
        gameQuestionService.saveQuestion(gameQuestionRequest);

        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @GetMapping("/game/questions")
    public ResponseEntity<ApiResponse<List<GameQuestionResponse>>> getRandomGameQuestions() {
        return ResponseEntity.ok(ApiResponse.of(gameQuestionService.getRandomQuestionsByLanguage()));
    }
}
