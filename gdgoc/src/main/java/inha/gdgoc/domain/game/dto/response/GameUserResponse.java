package inha.gdgoc.domain.game.dto.response;

import inha.gdgoc.domain.game.entity.GameUser;
import lombok.Getter;

@Getter
public class GameUserResponse {
    private Long id;
    private int rank;
    private String name;
    private String major;
    private double typingSpeed;

    public GameUserResponse(int rank, GameUser gameUser) {
        this.id = gameUser.getId();
        this.rank = rank;
        this.name = gameUser.getName();
        this.major = gameUser.getMajor();
        this.typingSpeed = gameUser.getTypingSpeed();
    }
}
