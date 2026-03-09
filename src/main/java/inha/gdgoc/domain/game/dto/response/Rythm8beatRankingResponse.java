package inha.gdgoc.domain.game.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Rythm8beatRankingResponse {
    private List<Rythm8beatRankItemResponse> top3;
    private Rythm8beatRankItemResponse userRank;
}
