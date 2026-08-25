package inha.gdgoc.domain.resource.enums;

import lombok.Getter;

@Getter
public enum S3KeyType {

    study("study"),
    recruitCore("recruit/core"),
    recruitMember("recruit/member"),
    profile("profile"),
    boardEvent("board/event"),
    boardNotice("board/notice"),
    boardFree("board/free"),
    landing("landing");

    private final String value;

    S3KeyType(String value) {
        this.value = value;
    }
}
