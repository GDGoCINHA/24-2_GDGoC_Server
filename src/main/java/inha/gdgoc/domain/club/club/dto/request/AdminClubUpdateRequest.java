package inha.gdgoc.domain.club.club.dto.request;

import inha.gdgoc.domain.club.club.enums.ClubStatus;
import jakarta.validation.Valid;

/** 운영진 수정. 이끔이가 고치는 항목 + 상태·기수. */
public record AdminClubUpdateRequest(@Valid ClubUpdateRequest club, ClubStatus status, Long termId) {}
