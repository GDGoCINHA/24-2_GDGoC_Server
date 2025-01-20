package inha.gdgoc.domain.user.entity;

import lombok.Getter;

@Getter
public enum CoreType {
    // 테크 회의 후 타입 추가 필요
    LEAD("Lead"),
    VICE_LEAD("Vice Lead"),
    HR_LEAD("HR Lead"),
    HR_CORE("HR Core"),
    TECH_LEAD("Tech Lead"),
    TECH_CORE("Tech Core"),
    MEMBER("Meber");

    private final String core;

    CoreType(String core){
        this.core = core;
    }
}
