package inha.gdgoc.domain.club.leader.dto.response;

import java.time.Instant;
import java.util.List;

/** 이끔이 권한 보유자 한 줄. {@code clubNames} 는 지금 이끄는 소모임. */
public record ClubLeaderGrantResponse(
    Long userId, String name, String major, Instant grantedAt, List<String> clubNames) {}
