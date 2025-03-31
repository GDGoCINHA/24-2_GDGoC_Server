package inha.gdgoc.domain.recruit.dto.response;

import inha.gdgoc.global.common.BaseEntity;

public class SpecifiedMemberResponse extends BaseEntity {
    private String name;
    private String major;
    private String studentId;
    private boolean isPayed;

    public SpecifiedMemberResponse(String name, String major, String studentId, boolean isPayed) {
        this.name = name;
        this.major = major;
        this.studentId = studentId;
        this.isPayed = isPayed;
    }
}
