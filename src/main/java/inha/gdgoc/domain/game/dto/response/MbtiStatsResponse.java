package inha.gdgoc.domain.game.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MbtiStatsResponse {
    private final long totalCount;
    private final List<MbtiTypeCountResponse> typeCounts;
}
