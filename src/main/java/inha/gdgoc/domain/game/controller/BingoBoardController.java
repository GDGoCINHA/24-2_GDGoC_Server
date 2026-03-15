package inha.gdgoc.domain.game.controller;

import static inha.gdgoc.domain.game.controller.message.GameUserMessage.BINGO_BOARD_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.game.controller.message.GameUserMessage.BINGO_BOARDS_RETRIEVED_SUCCESS;

import inha.gdgoc.domain.game.dto.response.BingoBoardResponse;
import inha.gdgoc.domain.game.service.BingoBoardService;
import inha.gdgoc.global.dto.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/game/bingo")
@RequiredArgsConstructor
@RestController
public class BingoBoardController {

    private final BingoBoardService bingoBoardService;

    @GetMapping("/boards")
    public ResponseEntity<ApiResponse<List<BingoBoardResponse>, Void>> getBoards() {
        List<BingoBoardResponse> response = bingoBoardService.findAllBoards();
        return ResponseEntity.ok(ApiResponse.ok(BINGO_BOARDS_RETRIEVED_SUCCESS, response));
    }

    @GetMapping("/boards/{teamNumber}")
    public ResponseEntity<ApiResponse<BingoBoardResponse, Void>> getBoard(
            @PathVariable Integer teamNumber
    ) {
        BingoBoardResponse response = bingoBoardService.findBoard(teamNumber);
        return ResponseEntity.ok(ApiResponse.ok(BINGO_BOARD_RETRIEVED_SUCCESS, response));
    }
}
