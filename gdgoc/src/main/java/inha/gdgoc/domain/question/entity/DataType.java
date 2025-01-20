package inha.gdgoc.domain.question.entity;

import lombok.Getter;

@Getter
public enum DataType {
    TEXT("텍스트 답변"),
    SINGLE_CHOICE("단일 선택"),
    MULTI_CHOICE("다중 선택"),
    SCALE("척도형");

    private final String dataType;

    DataType(String dataType) {
        this.dataType = dataType;
    }
}
