package inha.gdgoc.domain.club.member.service;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.dto.response.ClubMemberAddResponse;
import inha.gdgoc.domain.club.member.dto.response.ClubMemberCandidateResponse;
import inha.gdgoc.domain.club.member.dto.response.ClubMemberResponse;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import inha.gdgoc.domain.club.member.repository.ClubMemberRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참여 신청·승인·탈퇴·강퇴·리더 교체·일괄 추가.
 *
 * <p>막는 것은 중복 신청, 모집 중이 아닐 때 신청, 위임 전 리더 탈퇴뿐이다. 정원 초과 승인은 막지 않는다(경고는 화면이 한다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMemberService {

  private static final List<ClubMemberStatus> LIVE =
      List.of(ClubMemberStatus.PENDING, ClubMemberStatus.ACTIVE);

  /** 소모임에 들어올 수 있는 역할 — 참여 신청(MEMBER 이상)과 같은 기준이다. */
  private static final List<UserRole> ELIGIBLE =
      Arrays.stream(UserRole.values())
          .filter(role -> UserRole.hasAtLeast(role, UserRole.MEMBER))
          .toList();

  private static final int CANDIDATE_LIMIT = 20;

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

  /**
   * 멤버 추가 화면의 이름 검색. 리더와 운영진만 부른다. 부원(MEMBER 이상)만 찾는다.
   *
   * <p>검색어가 비면 빈 목록이다 — 부원 전체 명단을 내주지 않는다.
   */
  public List<ClubMemberCandidateResponse> findCandidates(
      Long clubId, Long userId, boolean staff, String query) {
    clubAccessService.requireLeaderOrStaff(clubId, userId, staff);
    String keyword = query == null ? "" : query.trim();
    if (keyword.isEmpty()) {
      return List.of();
    }
    List<User> users =
        userRepository.searchByName(keyword, ELIGIBLE, PageRequest.of(0, CANDIDATE_LIMIT));
    if (users.isEmpty()) {
      return List.of();
    }
    Map<Long, ClubMember> live = findLive(clubId, users.stream().map(User::getId).toList());
    return users.stream()
        .map(
            u -> {
              ClubMember m = live.get(u.getId());
              return ClubMemberCandidateResponse.of(u, m == null ? null : m.getStatus());
            })
        .toList();
  }

  /**
   * 리더·운영진이 여러 명을 한 번에 넣는다. 수락 단계 없이 바로 ACTIVE 다.
   *
   * <p>모집 마감·정원·승인 전 상태와 무관하게 넣는다. 신청 중이면 승인하고, 이미 멤버면 건너뛴다.
   */
  @Transactional
  public ClubMemberAddResponse addMembers(
      Long clubId, Long userId, boolean staff, List<Long> userIds) {
    Club club = clubAccessService.requireLeaderOrStaff(clubId, userId, staff);
    List<Long> ids = userIds.stream().distinct().toList();
    List<User> users = userRepository.findAllById(ids);
    if (users.size() != ids.size()) {
      throw new BusinessException(ClubErrorCode.USER_NOT_FOUND);
    }
    if (users.stream().anyMatch(u -> !UserRole.hasAtLeast(u.getUserRole(), UserRole.MEMBER))) {
      throw new BusinessException(ClubErrorCode.TARGET_NOT_MEMBER_ROLE);
    }
    Map<Long, ClubMember> live = findLive(clubId, ids);
    Instant now = Instant.now();
    int added = 0;
    int approved = 0;
    for (User user : users) {
      ClubMember existing = live.get(user.getId());
      if (existing == null) {
        clubMemberRepository.save(ClubMember.founder(club, user, now));
        added++;
      } else if (existing.getStatus() == ClubMemberStatus.PENDING) {
        existing.approve(now);
        approved++;
      }
    }
    return new ClubMemberAddResponse(added, approved, users.size() - added - approved);
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

  /** userId → 신청 중이거나 참여 중인 행. */
  private Map<Long, ClubMember> findLive(Long clubId, List<Long> userIds) {
    return clubMemberRepository.findAllByClubIdAndUserIdInAndStatusIn(clubId, userIds, LIVE).stream()
        .collect(Collectors.toMap(m -> m.getUser().getId(), m -> m, (a, b) -> a));
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
