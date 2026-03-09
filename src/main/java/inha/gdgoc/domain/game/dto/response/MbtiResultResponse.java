package inha.gdgoc.domain.game.dto.response;

import inha.gdgoc.domain.game.entity.MbtiResult;
import lombok.Getter;

@Getter
public class MbtiResultResponse {
    private final Long id;
    private final String name;
    private final String studentId;
    private final String mbtiType;

    public MbtiResultResponse(MbtiResult mbtiResult) {
        this.id = mbtiResult.getId();
        this.name = mbtiResult.getName();
        this.studentId = mbtiResult.getStudentId();
        this.mbtiType = mbtiResult.getMbtiType();
    }
}
