package inha.gdgoc.domain.club.completion.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 쉬는 주 목록 통째 저장. 아무 요일이나 받아 그 주 월요일로 맞춘다. 개수·시점은 제한하지 않는다 — 크기 제한은 요청 하나가 비정상적으로 커지는 것만
 * 막는다.
 */
public record ClubRestWeeksRequest(@NotNull @Size(max = 200) List<@NotNull LocalDate> weeks) {}
