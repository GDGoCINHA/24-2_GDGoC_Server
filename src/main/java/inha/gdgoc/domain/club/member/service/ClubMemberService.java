package inha.gdgoc.domain.club.member.service;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.dto.response.ClubMemberResponse;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import inha.gdgoc.domain.club.member.repository.ClubMemberRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참여 신청·승인·탈퇴·강퇴·리더 교체.
 *
 * <p>막는 것은 중복 신청, 모집 중이 아닐 때 신청, 위임 전 리더 탈퇴뿐이다. 정원 초과 승인은 막지 않는다(경고는 화면이 한다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberService {

  private static final List<ClubMemberStatus> LIVE =
      List.of(ClubMemberStatus.PENDING, ClubMemberStatus.ACTIVE);

  private final ClubAccessService clubAccessService;
  private final ClubMemberRepository clubMemberRepository;
  private final UserRepository userRepository;

  @Transactional
  public void apply(Long clubId, Long userId, String message) {
    Club club = clubAccessService.getClub(clubId);
    if (!club.isRecruiting()) {
      throw new BusinessException(ClubErrorCode.NOT_RECRUITING);
    }
    if (clubMemberRepository.existsByClubIdAndUserIdAndStatusIn(clubId, userId, LIVE)) {
      throw new BusinessException(ClubErrorCode.ALREADY_APPLIED);
    }
    clubMemberRepository.save(ClubMember.apply(club, getUser(userId), message, Instant.now()));
  }

  /** 신청 취소 또는 탈퇴. 리더는 넘기기 전에는 나갈 수 없다. */
  @Transactional
  public void leave(Long clubId, Long userId) {
    Club club = clubAccessService.getClub(clubId);
    ClubMember me =
        clubMemberRepository
            .findFirstByClubIdAndUserIdAndStatusIn(clubId, userId, LIVE)
            .orElseThrow(() -> new BusinessException(ClubErrorCode.MEMBER_NOT_FOUND));
    if (me.getStatus() == ClubMemberStatus.PENDING) {
      me.cancel();
      return;
    }
    if (userId.equals(club.getLeader().getId())) {
      throw new BusinessException(ClubErrorCode.LEADER_CANNOT_LEAVE);
    }
    me.leave(Instant.now());
  }

  /** 멤버 명단. 멤버와 운영진만 본다. */
  public List<ClubMemberResponse> findActive(Long clubId, Long userId, boolean staff) {
    Club club = clubAccessService.getClub(clubId);
    if (!staff) {
      clubAccessService.requireActiveMember(clubId, userId);
    }
    return toResponses(club, ClubMemberStatus.ACTIVE);
  }

  public List<ClubMemberResponse> findPending(Long clubId, Long userId) {
    return toResponses(clubAccessService.requireLeader(clubId, userId), ClubMemberStatus.PENDING);
  }

  /** 승인. 정원을 넘어도 승인한다. */
  @Transactional
  public void approve(Long clubId, Long memberId, Long userId) {
    clubAccessService.requireLeader(clubId, userId);
    getInState(clubId, memberId, ClubMemberStatus.PENDING).approve(Instant.now());
  }

  @Transactional
  public void reject(Long clubId, Long memberId, Long userId) {
    clubAccessService.requireLeader(clubId, userId);
    getInState(clubId, memberId, ClubMemberStatus.PENDING).reject();
  }

  @Transactional
  public void kick(Long clubId, Long memberId, Long userId) {
    clubAccessService.requireLeader(clubId, userId);
    ClubMember target = getInState(clubId, memberId, ClubMemberStatus.ACTIVE);
    if (target.getUser().getId().equals(userId)) {
      throw new BusinessException(ClubErrorCode.CANNOT_KICK_SELF);
    }
    target.kick(Instant.now());
  }

  /** 리더가 다른 ACTIVE 멤버에게 넘긴다. 넘긴 사람은 일반 멤버로 남는다. */
  @Transactional
  public void changeLeader(Long clubId, Long userId, Long newLeaderId) {
    Club club = clubAccessService.requireLeader(clubId, userId);
    if (!clubAccessService.isActiveMember(clubId, newLeaderId)) {
      throw new BusinessException(ClubErrorCode.TARGET_NOT_ACTIVE_MEMBER);
    }
    club.changeLeader(getUser(newLeaderId));
  }

  /** 운영진 교체. 대상이 멤버가 아니면 바로 ACTIVE 로 넣고, 신청 중이면 승인한다. */
  @Transactional
  public void changeLeaderByStaff(Long clubId, Long newLeaderId) {
    Club club = clubAccessService.getClub(clubId);
    User newLeader = getUser(newLeaderId);
    Instant now = Instant.now();
    clubMemberRepository
        .findFirstByClubIdAndUserIdAndStatusIn(clubId, newLeaderId, LIVE)
        .ifPresentOrElse(
            m -> {
              if (m.getStatus() == ClubMemberStatus.PENDING) {
                m.approve(now);
              }
            },
            () -> clubMemberRepository.save(ClubMember.founder(club, newLeader, now)));
    club.changeLeader(newLeader);
  }

  private ClubMember getInState(Long clubId, Long memberId, ClubMemberStatus expected) {
    ClubMember member =
        clubMemberRepository
            .findById(memberId)
            .filter(m -> m.getClub().getId().equals(clubId))
            .orElseThrow(() -> new BusinessException(ClubErrorCode.MEMBER_NOT_FOUND));
    if (member.getStatus() != expected) {
      throw new BusinessException(ClubErrorCode.INVALID_MEMBER_STATE);
    }
    return member;
  }

  private List<ClubMemberResponse> toResponses(Club club, ClubMemberStatus status) {
    Long leaderId = club.getLeader().getId();
    return clubMemberRepository.findAllWithUser(club.getId(), status).stream()
        .map(m -> ClubMemberResponse.of(m, leaderId))
        .toList();
  }

  private User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ClubErrorCode.USER_NOT_FOUND));
  }
}
