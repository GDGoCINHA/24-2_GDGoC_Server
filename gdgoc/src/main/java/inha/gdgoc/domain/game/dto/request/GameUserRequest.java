package inha.gdgoc.domain.game.dto.request;

import inha.gdgoc.domain.game.entity.GameUser;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameUserRequest {
    private String name;
    private String major;
    private String studentId;
    private String phoneNumber;
    private double typingSpeed;

    public GameUser toEntity() {
        return GameUser.builder()
                .name(name)
                .major(major)
                .studentId(studentId)
                .phoneNumber(phoneNumber)
                .typingSpeed(typingSpeed)
                .build();
    }
}
