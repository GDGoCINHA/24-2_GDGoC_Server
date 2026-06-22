package inha.gdgoc.domain.board.notice.enums;

import lombok.Getter;

@Getter
public enum CategoryEnum {
    OPERATION("운영"),
    SCHEDULE("일정"),
    RECRUIT("모집"),
    ETC("기타");

    private final String description;

    CategoryEnum(String description) {
        this.description = description;
    }
}
