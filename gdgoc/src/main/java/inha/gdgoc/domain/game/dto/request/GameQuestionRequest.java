package inha.gdgoc.domain.game.dto.request;

import inha.gdgoc.domain.game.entity.GameQuestion;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class GameQuestionRequest {
    private String language;
    private String content;

    public GameQuestion toEntity() {
        return GameQuestion.builder()
                .language(language)
                .content(content)
                .build();
    }
}
