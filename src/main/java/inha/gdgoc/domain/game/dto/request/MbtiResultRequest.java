package inha.gdgoc.domain.game.dto.request;

import inha.gdgoc.domain.game.entity.MbtiResult;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MbtiResultRequest {
    private String name;
    private String studentId;
    private String mbtiType;

    public MbtiResult toEntity() {
        return MbtiResult.builder()
                .name(name)
                .studentId(studentId)
                .mbtiType(mbtiType)
                .build();
    }
}
