package inha.gdgoc.domain.club.leader.service;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.repository.ClubRepository;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.leader.dto.request.ClubOpenRequestCreateRequest;
import inha.gdgoc.domain.club.leader.dto.response.ClubLeaderGrantResponse;
import inha.gdgoc.domain.club.leader.dto.response.ClubOpenRequestResponse;
import inha.gdgoc.domain.club.leader.entity.ClubLeaderGrant;
import inha.gdgoc.domain.club.leader.entity.ClubOpenRequest;
import inha.gdgoc.domain.club.leader.enums.ClubOpenRequestStatus;
import inha.gdgoc.domain.club.leader.repository.ClubLeaderGrantRepository;
import inha.gdgoc.domain.club.leader.repository.ClubOpenRequestRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 리더 권한과 개설 신청. 권한은 「새 소모임을 열 수 있는가」만 정한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubLeaderService {

  private final ClubAccessService clubAccessService;
  private final ClubLeaderGrantRepository clubLeaderGrantRepository;
  private final ClubOpenRequestRepository clubOpenRequestRepository;
  private final ClubRepository clubRepository;
  private final UserRepository userRepository;

  public boolean hasGrant(Long userId) {
    return clubAccessService.hasLeaderGrant(userId);
  }

  @Transactional
  public Long requestOpen(Long userId, ClubOpenRequestCreateRequest req) {
    ClubOpenRequest request =
        ClubOpenRequest.create(
            getUser(userId), req.name(), req.category(), req.summary(), req.goal());
    return clubOpenRequestRepository.save(request).getId();
  }

  public List<ClubOpenRequestResponse> findMyRequests(Long userId) {
    return clubOpenRequestRepository.findAllByUserIdOrderByIdDesc(userId).stream()
        .map(ClubOpenRequestResponse::of)
        .toList();
  }

  public List<ClubOpenRequestResponse> findRequests(ClubOpenRequestStatus status) {
    return clubOpenRequestRepository.findAllWithUser(status).stream()
        .map(ClubOpenRequestResponse::of)
        .toList();
  }

  /** 승인하면 권한을 준다. 이미 있으면 그대로 둔다. 소모임 자체는 신청자가 직접 연다. */
  @Transactional
  public void approve(Long requestId, Long staffId) {
    ClubOpenRequest request = getPending(requestId);
    Instant now = Instant.now();
    request.approve(staffId, now);
    grantIfAbsent(request.getUser(), staffId, now);
  }

  @Transactional
  public void reject(Long requestId, Long staffId, String reason) {
    getPending(requestId).reject(staffId, reason, Instant.now());
  }

  @Transactional
  public void grant(Long userId, Long staffId) {
    grantIfAbsent(getUser(userId), staffId, Instant.now());
  }

  /** 회수해도 이미 이끄는 소모임은 그대로다. 권한이 없으면 아무 일도 하지 않는다. */
  @Transactional
  public void revoke(Long userId) {
    clubLeaderGrantRepository
        .findFirstByUserIdAndRevokedAtIsNull(userId)
        .ifPresent(g -> g.revoke(Instant.now()));
  }

  public List<ClubLeaderGrantResponse> findGrants() {
    List<ClubLeaderGrant> grants = clubLeaderGrantRepository.findAllActiveWithUser();
    if (grants.isEmpty()) {
      return List.of();
    }
    Map<Long, List<String>> clubNames =
        clubRepository
            .findAllByLeaderIdIn(grants.stream().map(g -> g.getUser().getId()).toList())
            .stream()
            .collect(
                Collectors.groupingBy(
                    c -> c.getLeader().getId(),
                    Collectors.mapping(Club::getName, Collectors.toList())));
    return grants.stream()
        .map(
            g ->
                new ClubLeaderGrantResponse(
                    g.getUser().getId(),
                    g.getUser().getName(),
                    g.getUser().getMajor(),
                    g.getGrantedAt(),
                    clubNames.getOrDefault(g.getUser().getId(), List.of())))
        .toList();
  }

  private void grantIfAbsent(User user, Long staffId, Instant now) {
    if (!clubLeaderGrantRepository.existsByUserIdAndRevokedAtIsNull(user.getId())) {
      clubLeaderGrantRepository.save(ClubLeaderGrant.grant(user, staffId, now));
    }
  }

  private ClubOpenRequest getPending(Long requestId) {
    ClubOpenRequest request =
        clubOpenRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new BusinessException(ClubErrorCode.OPEN_REQUEST_NOT_FOUND));
    if (request.getStatus() != ClubOpenRequestStatus.PENDING) {
      throw new BusinessException(ClubErrorCode.OPEN_REQUEST_ALREADY_HANDLED);
    }
    return request;
  }

  private User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ClubErrorCode.USER_NOT_FOUND));
  }
}
