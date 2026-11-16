package inha.gdgoc.domain.homecoming.service;

import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.HomecomingMemberRole;
import inha.gdgoc.domain.homecoming.enums.HomecomingTeam;

/**
 * 검증·정리를 마친 신청 내용. {@link HomecomingProfileNormalizer} 만 만든다.
 *
 * <p>참석 유형이 받지 않는 칸은 이미 비워져 있다 — 화면에서 유형을 바꾸며 남은 값이 저장·집계에 섞이지 않게 하는 마지막 방어선이다.
 */
public record HomecomingProfile(
    AttendType attendType,
    String name,
    String phone,
    Integer yearStart,
    Integer yearEnd,
    HomecomingMemberRole memberRole,
    HomecomingTeam team,
    boolean late,
    String arrivalTime,
    String message) {}
