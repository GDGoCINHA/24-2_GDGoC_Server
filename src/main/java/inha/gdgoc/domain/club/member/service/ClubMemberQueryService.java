package inha.gdgoc.domain.club.member.service;

import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import inha.gdgoc.domain.club.member.repository.ClubMemberRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 멤버 명단 조회. B(출석 명단)와 C(4명 판정)가 부른다.
 *
 * <p>명단의 날짜 경계는 한국 시간 기준이다 — 활동일은 사람이 고른 날짜이고, 그 날짜는 한국 날짜다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberQueryService {

  static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final ClubMemberRepository clubMemberRepository;

  /**
   * 그 날짜의 팀 명단. 그날 합류한 사람은 들고, 그날 나간 사람은 빠진다.
   *
   * <p>한 팀의 멤버 행은 많아야 수십 개라 메모리에서 거른다. 경계 규칙을 {@link ClubMember#wasActiveOn} 한 곳에 두려는 것이다.
   */
  public List<ClubMember> findActiveOn(Long clubId, LocalDate date) {
    return clubMemberRepository.findAllByClubIdAndJoinedAtIsNotNull(clubId).stream()
        .filter(member -> member.wasActiveOn(date, KST))
        .toList();
  }

  /** 지금 팀원 수 (이끔이 포함). */
  public long countActive(Long clubId) {
    return clubMemberRepository.countByClubIdAndStatus(clubId, ClubMemberStatus.ACTIVE);
  }
}
