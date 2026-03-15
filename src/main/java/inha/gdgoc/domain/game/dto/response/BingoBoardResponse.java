package inha.gdgoc.domain.game.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class BingoBoardResponse {
    private final Integer teamNumber;
    private final List<String> marks;
    private final int checkedCount;
    private final int rank;

    public BingoBoardResponse(
            Integer teamNumber,
            List<String> marks,
            int checkedCount,
            int rank
    ) {
        this.teamNumber = teamNumber;
        this.marks = marks;
        this.checkedCount = checkedCount;
        this.rank = rank;
    }
}
