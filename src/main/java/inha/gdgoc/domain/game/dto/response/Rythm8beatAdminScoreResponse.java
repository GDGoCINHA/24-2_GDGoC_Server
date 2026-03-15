package inha.gdgoc.domain.game.dto.response;

import inha.gdgoc.domain.game.entity.Rythm8beatScore;
import java.time.Instant;
import lombok.Getter;

@Getter
public class Rythm8beatAdminScoreResponse {
    private final int rank;
    private final Long id;
    private final String phoneNumber;
    private final String nickname;
    private final int score;
    private final int stageReached;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Rythm8beatAdminScoreResponse(int rank, Rythm8beatScore score) {
        this.rank = rank;
        this.id = score.getId();
        this.phoneNumber = score.getPhoneNumber();
        this.nickname = score.getNickname();
        this.score = score.getScore();
        this.stageReached = score.getStageReached();
        this.createdAt = score.getCreatedAt();
        this.updatedAt = score.getUpdatedAt();
    }
}
