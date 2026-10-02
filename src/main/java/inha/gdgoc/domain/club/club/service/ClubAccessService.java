package inha.gdgoc.domain.club.club.service;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.repository.ClubRepository;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.leader.repository.ClubLeaderGrantRepository;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import inha.gdgoc.domain.club.member.repository.ClubMemberRepository;
import inha.gdgoc.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소모임 단위 권한 검사. B·C 가 서비스 첫 줄에서 부른다.
 *
 * <p>역할(MEMBER·CORE) 검사는 여기서 하지 않는다 — 컨트롤러의 {@code @Authorize} 가 한다. 여기는 「이 팀의 멤버인가 / 이끔이인가」처럼
 * 데이터로만 알 수 있는 것을 본다. 이끔이는 {@code users.role} 이 아니라 {@code club.leader_id} 다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubAccessService {

  private final ClubRepository clubRepository;
  private final ClubMemberRepository clubMemberRepository;
  private final ClubLeaderGrantRepository clubLeaderGrantRepository;

  public Club getClub(Long clubId) {
    return clubRepository
        .findById(clubId)
        .orElseThrow(() -> new BusinessException(ClubErrorCode.CLUB_NOT_FOUND));
  }

  public boolean isActiveMember(Long clubId, Long userId) {
    return clubMemberRepository.existsByClubIdAndUserIdAndStatus(
        clubId, userId, ClubMemberStatus.ACTIVE);
  }

  public void requireActiveMember(Long clubId, Long userId) {
    getClub(clubId);
    if (!isActiveMember(clubId, userId)) {
      throw new BusinessException(ClubErrorCode.NOT_CLUB_MEMBER);
    }
  }

  public boolean isLeader(Long clubId, Long userId) {
    return userId != null && userId.equals(getClub(clubId).getLeader().getId());
  }

  /** 이끔이가 아니면 403. 소모임이 없으면 404 가 먼저 난다. */
  public Club requireLeader(Long clubId, Long userId) {
    Club club = getClub(clubId);
    if (userId == null || !userId.equals(club.getLeader().getId())) {
      throw new BusinessException(ClubErrorCode.NOT_CLUB_LEADER);
    }
    return club;
  }

  /** 소모임을 새로 열 수 있는가 — 회수되지 않은 이끔이 권한이 있는가. */
  public boolean hasLeaderGrant(Long userId) {
    return clubLeaderGrantRepository.existsByUserIdAndRevokedAtIsNull(userId);
  }

  public void requireLeaderGrant(Long userId) {
    if (!hasLeaderGrant(userId)) {
      throw new BusinessException(ClubErrorCode.LEADER_GRANT_REQUIRED);
    }
  }
}
