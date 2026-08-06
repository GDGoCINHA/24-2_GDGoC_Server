package inha.gdgoc.domain.recruit.member.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum RecruitMemberErrorCode implements ErrorCode {

    // 403 FORBIDDEN — 코어의 RECRUITMENT_NOT_OPEN·RECRUITMENT_CLOSED 와 같은 상태다.
    RECRUIT_MEMBER_NOT_OPEN(HttpStatus.FORBIDDEN, "부원 모집 기간이 아직 시작되지 않았습니다."),
    RECRUIT_MEMBER_CLOSED(HttpStatus.FORBIDDEN, "부원 모집 기간이 종료되었습니다."),

    // 409 CONFLICT
    RECRUIT_MEMBER_ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 지원을 완료하였습니다."),

    // 404 NOT FOUND
    RECRUIT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 멤버를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
