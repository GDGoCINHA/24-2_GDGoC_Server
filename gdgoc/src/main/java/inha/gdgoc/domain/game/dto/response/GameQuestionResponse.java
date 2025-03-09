package inha.gdgoc.domain.game.dto.response;

import inha.gdgoc.domain.game.entity.GameQuestion;
import lombok.Getter;

@Getter
public class GameQuestionResponse {

    private Long id;
    private String language;
    private String content;

    public GameQuestionResponse(GameQuestion gameQuestion) {
        this.id = gameQuestion.getId();
        this.language = gameQuestion.getLanguage();
        this.content = gameQuestion.getContent();
    }
}
