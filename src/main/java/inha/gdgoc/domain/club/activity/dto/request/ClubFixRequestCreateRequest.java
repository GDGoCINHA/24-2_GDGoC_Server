package inha.gdgoc.domain.club.activity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 출석 수정 요청. 바꿀 값은 서버가 정한다 — 지금 출석이면 결석으로, 결석이면 출석으로. */
public record ClubFixRequestCreateRequest(@NotBlank @Size(max = 1000) String reason) {}
