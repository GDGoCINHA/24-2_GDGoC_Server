package inha.gdgoc.domain.club.club.service;

import inha.gdgoc.domain.club.club.dto.request.AdminClubUpdateRequest;
import inha.gdgoc.domain.club.club.dto.request.ClubCreateRequest;
import inha.gdgoc.domain.club.club.dto.request.ClubUpdateRequest;
import inha.gdgoc.domain.club.club.dto.response.ClubDetailResponse;
import inha.gdgoc.domain.club.club.dto.response.ClubMembershipResponse;
import inha.gdgoc.domain.club.club.dto.response.ClubSummaryResponse;
import inha.gdgoc.domain.club.club.dto.response.MyClubResponse;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubRecruitStatus;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.club.repository.ClubRepository;
import inha.gdgoc.domain.club.club.repository.ClubTermRepository;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import inha.gdgoc.domain.club.member.repository.ClubMemberRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 소모임 개설·조회·수정. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

  private static final List<ClubMemberStatus> MINE =
      List.of(ClubMemberStatus.PENDING, ClubMemberStatus.ACTIVE);

  private final ClubAccessService clubAccessService;
  private final ClubRepository clubRepository;
  private final ClubTermRepository clubTermRepository;
  private final ClubMemberRepository clubMemberRepository;
  private final UserRepository userRepository;

  /** 목록. 숨김 소모임은 운영진에게만 보인다. */
  public Page<ClubSummaryResponse> search(
      ClubCategory category,
      ClubRecruitStatus recruitStatus,
      String keyword,
      boolean staff,
      Pageable pageable) {
    ClubStatus hidden = staff ? null : ClubStatus.HIDDEN;
    String kw = keyword == null ? "" : keyword.trim();
    Page<Club> page = clubRepository.search(hidden, category, recruitStatus, kw, pageable);
    Map<Long, Long> counts = countActive(page.getContent().stream().map(Club::getId).toList());
    return page.map(c -> ClubSummaryResponse.of(c, counts.getOrDefault(c.getId(), 0L)));
  }

  public List<MyClubResponse> findMine(Long userId) {
    List<ClubMember> mine = clubMemberRepository.findMine(userId, MINE);
    Map<Long, Long> counts = countActive(mine.stream().map(m -> m.getClub().getId()).toList());
    return mine.stream()
        .map(
            m ->
                new MyClubResponse(
                    ClubSummaryResponse.of(m.getClub(), counts.getOrDefault(m.getClub().getId(), 0L)),
                    m.getStatus(),
                    userId.equals(m.getClub().getLeader().getId())))
        .toList();
  }

  /** 상세. 단톡방 링크는 ACTIVE 멤버와 운영진에게만 준다. {@code userId} 는 비로그인이면 null. */
  public ClubDetailResponse getDetail(Long clubId, Long userId, boolean staff) {
    Club club = clubAccessService.getClub(clubId);
    // 비로그인이면 userId 가 null 이다 — 신청 상태도 없고 링크도 받지 않는다.
    ClubMembershipResponse membership =
        userId == null
            ? null
            : clubMemberRepository
                .findFirstByClubIdAndUserIdAndStatusIn(clubId, userId, MINE)
                .map(
                    m ->
                        new ClubMembershipResponse(
                            m.getStatus(), userId.equals(club.getLeader().getId())))
                .orElse(null);
    if (club.getStatus() == ClubStatus.HIDDEN && !staff && membership == null) {
      throw new BusinessException(ClubErrorCode.CLUB_NOT_FOUND);
    }
    boolean active = membership != null && membership.status() == ClubMemberStatus.ACTIVE;
    long count = clubMemberRepository.countByClubIdAndStatus(clubId, ClubMemberStatus.ACTIVE);
    return ClubDetailResponse.of(club, count, active || staff, membership);
  }

  /** 개설. 리더 권한이 있어야 하고, 개설자는 바로 ACTIVE 멤버가 된다. 기수를 안 주면 최신 기수. */
  @Transactional
  public Long create(Long userId, ClubCreateRequest req) {
    clubAccessService.requireLeaderGrant(userId);
    User leader = getUser(userId);
    ClubTerm term = req.termId() != null ? getTerm(req.termId()) : latestTerm();
    Club club =
        clubRepository.save(
            Club.create(
                term,
                leader,
                req.name(),
                req.category(),
                req.summary(),
                req.description(),
                req.activityMethod(),
                req.imageUrl(),
                req.kakaoLink(),
                req.capacity(),
                req.startDate(),
                req.endDate()));
    clubMemberRepository.save(ClubMember.founder(club, leader, Instant.now()));
    return club.getId();
  }

  @Transactional
  public void update(Long clubId, Long userId, ClubUpdateRequest req) {
    apply(clubAccessService.requireLeader(clubId, userId), req);
  }

  @Transactional
  public void updateByStaff(Long clubId, AdminClubUpdateRequest req) {
    Club club = clubAccessService.getClub(clubId);
    if (req.club() != null) {
      apply(club, req.club());
    }
    club.updateByStaff(req.status(), req.termId() != null ? getTerm(req.termId()) : null);
  }

  private void apply(Club club, ClubUpdateRequest req) {
    club.update(
        req.name(),
        req.category(),
        req.summary(),
        req.description(),
        req.activityMethod(),
        req.imageUrl(),
        req.kakaoLink(),
        req.capacity(),
        req.startDate(),
        req.endDate(),
        req.recruitStatus());
  }

  private Map<Long, Long> countActive(List<Long> clubIds) {
    Map<Long, Long> counts = new HashMap<>();
    if (clubIds.isEmpty()) {
      return counts;
    }
    for (Object[] row : clubMemberRepository.countByClubIds(clubIds, ClubMemberStatus.ACTIVE)) {
      counts.put((Long) row[0], (Long) row[1]);
    }
    return counts;
  }

  private User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ClubErrorCode.USER_NOT_FOUND));
  }

  private ClubTerm getTerm(Long termId) {
    return clubTermRepository
        .findById(termId)
        .orElseThrow(() -> new BusinessException(ClubErrorCode.TERM_NOT_FOUND));
  }

  private ClubTerm latestTerm() {
    return clubTermRepository
        .findTopByOrderByIdDesc()
        .orElseThrow(() -> new BusinessException(ClubErrorCode.TERM_NOT_FOUND));
  }
}
