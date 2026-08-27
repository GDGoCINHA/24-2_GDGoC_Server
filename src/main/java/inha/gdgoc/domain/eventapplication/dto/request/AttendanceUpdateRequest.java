package inha.gdgoc.domain.eventapplication.dto.request;

import inha.gdgoc.domain.eventapplication.enums.EventAttendanceStatus;
import jakarta.validation.constraints.NotNull;

/** 운영진의 수기 참석 처리. QR 체크인 시각은 건드리지 않는다. */
public record AttendanceUpdateRequest(@NotNull EventAttendanceStatus status) {}
