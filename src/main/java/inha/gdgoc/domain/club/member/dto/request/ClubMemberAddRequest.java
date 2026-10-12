package inha.gdgoc.domain.club.member.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 리더·운영진이 여러 명을 한 번에 넣는다. 사용자 id 목록이다(club_member 행 id 가 아니다). */
public record ClubMemberAddRequest(@NotEmpty @Size(max = 100) List<@NotNull Long> userIds) {}
