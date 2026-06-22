package inha.gdgoc.domain.board.notice.enums;

import lombok.Getter;

@Getter
public enum ArticleStatusEnum {
    DELETED("삭제됨"),
    PENDING("보류"),
    PUBLISHED("발행됨");

    private final String description;

    ArticleStatusEnum(String description) {
        this.description = description;
    }
}
