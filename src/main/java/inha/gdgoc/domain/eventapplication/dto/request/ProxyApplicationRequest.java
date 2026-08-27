package inha.gdgoc.domain.eventapplication.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 신청 없이 현장에 온 사람을 운영진이 대신 등록한다.
 *
 * <p>마감과 정원을 넘겨도 등록된다. 현장 판단이 폼 설정보다 우선이기 때문이다. 답변은 받지 않으므로 필수 질문이 있어도 비어 있는 채로 남는다.
 */
public record ProxyApplicationRequest(@NotNull Long userId, boolean markAttended) {}
