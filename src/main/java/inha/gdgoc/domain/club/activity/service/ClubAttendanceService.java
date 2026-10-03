package inha.gdgoc.domain.club.activity.service;

import inha.gdgoc.domain.club.activity.dto.response.ClubFixRequestResponse;
import inha.gdgoc.domain.club.activity.dto.response.MyAttendanceResponse;
import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.entity.ClubAttendanceFixRequest;
import inha.gdgoc.domain.club.activity.enums.ClubFixRequestStatus;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.domain.club.activity.repository.ClubActivityAttendanceRepository;
import inha.gdgoc.domain.club.activity.repository.ClubActivityRepository;
import inha.gdgoc.domain.club.activity.repository.ClubAttendanceFixRequestRepository;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 출석 조회와 출석 수정 요청.
 *
 * <p>수락하면 출석을 <b>요청한 값으로 맞춘다</b>("반전" 이 아니다). 요청 뒤 리더가 기록을 직접 고쳤어도 수락이 그 수정을 다시 뒤집지 않는다.
 * 출석이 바뀌었으므로 기록은 확인 중으로 돌아가 운영진이 다시 본다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubAttendanceService {

  private final ClubAccessService clubAccessService;
  private final ClubActivityQueryService clubActivityQueryService;
  private final ClubActivityRepository clubActivityRepository;
  private final ClubActivityAttendanceRepository clubActivityAttendanceRepository;
  private final ClubAttendanceFixRequestRepository clubAttendanceFixRequestRepository;

  /** 이 소모임에서 내가 명단에 든 회차. 본인 것만 돌려주므로 따로 권한을 보지 않는다(탈퇴한 사람도 지난 기록을 본다). */
  public List<MyAttendanceResponse> myAttendance(Long clubId, Long userId) {
    clubAccessService.getClub(clubId);
    List<ClubActivityAttendance> rows = clubActivityAttendanceRepository.findMine(clubId, userId);
    if (rows.isEmpty()) {
      return List.of();
    }
    Set<Long> pending =
        new HashSet<>(
            clubAttendanceFixRequestRepository.findActivityIdsByUserAndStatus(
                userId,
                ClubFixRequestStatus.PENDING,
                rows.stream().map(r -> r.getActivity().getId()).toList()));
    return rows.stream()
        .map(
            r -> {
              ClubActivity activity = r.getActivity();
              return new MyAttendanceResponse(
                  activity.getId(),
                  activity.getActivityDate(),
                  activity.getSchedule() == null ? null : activity.getSchedule().getTitle(),
                  activity.getStatus(),
                  r.isAttended(),
                  pending.contains(activity.getId()));
            })
        .toList();
  }

  /** 본인 출석의 수정 요청. 그 기록의 명단에 있어야 하고, 인증 완료된 기록이거나 이미 대기 중인 요청이 있으면 409. */
  @Transactional
  public Long requestFix(Long activityId, Long userId, String reason) {
    ClubActivity activity =
        clubActivityRepository
            .findById(activityId)
            .orElseThrow(() -> new BusinessException(ClubActivityErrorCode.ACTIVITY_NOT_FOUND));
    ClubActivityAttendance mine =
        clubActivityAttendanceRepository
            .findByActivityIdAndUserId(activityId, userId)
            .orElseThrow(() -> new BusinessException(ClubActivityErrorCode.NOT_IN_ROSTER));
    activity.requireEditable();
    if (clubAttendanceFixRequestRepository.existsByActivityIdAndUserIdAndStatus(
        activityId, userId, ClubFixRequestStatus.PENDING)) {
      throw new BusinessException(ClubActivityErrorCode.FIX_REQUEST_ALREADY_PENDING);
    }

    ClubAttendanceFixRequest request =
        ClubAttendanceFixRequest.create(activity, mine.getUser(), !mine.isAttended(), reason.trim());
    try {
      clubAttendanceFixRequestRepository.saveAndFlush(request);
    } catch (DataIntegrityViolationException e) {
      // 위 검사를 지나 동시에 두 번 들어오면 운영 DB 의 부분 유니크 인덱스가 잡는다.
      throw new BusinessException(ClubActivityErrorCode.FIX_REQUEST_ALREADY_PENDING);
    }
    return request.getId();
  }

  /** 리더의 처리 목록. 기본은 처리 대기. */
  public List<ClubFixRequestResponse> listForLeader(
      Long clubId, Long userId, ClubFixRequestStatus status) {
    clubAccessService.requireLeader(clubId, userId);
    return clubAttendanceFixRequestRepository.findAllByClubAndStatus(clubId, status).stream()
        .map(
            r ->
                new ClubFixRequestResponse(
                    r.getId(),
                    r.getActivity().getId(),
                    r.getActivity().getActivityDate(),
                    r.getActivity().getStatus(),
                    r.getUser().getId(),
                    r.getUser().getName(),
                    clubActivityAttendanceRepository
                        .findByActivityIdAndUserId(r.getActivity().getId(), r.getUser().getId())
                        .map(ClubActivityAttendance::isAttended)
                        .orElse(null),
                    r.isRequestedAttended(),
                    r.getReason(),
                    r.getStatus(),
                    r.getCreatedAt()))
        .toList();
  }

  /**
   * 수락. 출석을 요청한 값으로 맞추고 기록을 확인 중으로 돌린다.
   *
   * <p><b>기록을 먼저 잠그고 읽는다.</b> 같은 트랜잭션에서 기록을 먼저 읽어 두면 잠금 조회가 이미 읽은 (낡은) 상태를 돌려줘, 그 사이 운영진이 인증
   * 완료한 것을 놓친다. 잠근 뒤에야 리더·잠금·명단을 확인한다.
   */
  @Transactional
  public void accept(Long requestId, Long userId) {
    ClubAttendanceFixRequest request = find(requestId);
    ClubActivity activity = clubActivityQueryService.getForUpdate(request.getActivity().getId());
    clubAccessService.requireLeader(activity.getClub().getId(), userId);
    if (!request.isPending()) {
      throw new BusinessException(ClubActivityErrorCode.FIX_REQUEST_ALREADY_HANDLED);
    }
    activity.requireEditable();
    ClubActivityAttendance row =
        clubActivityAttendanceRepository
            .findByActivityIdAndUserId(activity.getId(), request.getUser().getId())
            .orElseThrow(
                () -> new BusinessException(ClubActivityErrorCode.FIX_REQUEST_NOT_IN_ROSTER));

    Instant now = Instant.now();
    row.mark(request.isRequestedAttended());
    activity.reopenAfterAttendanceFix(now);
    request.accept(now);
  }

  /** 거절. 기록이 인증 완료됐어도 할 수 있다 — 남은 요청을 닫아야 한다. */
  @Transactional
  public void reject(Long requestId, Long userId) {
    ClubAttendanceFixRequest request = find(requestId);
    clubAccessService.requireLeader(request.getActivity().getClub().getId(), userId);
    request.reject(Instant.now());
  }

  private ClubAttendanceFixRequest find(Long requestId) {
    return clubAttendanceFixRequestRepository
        .findById(requestId)
        .orElseThrow(() -> new BusinessException(ClubActivityErrorCode.FIX_REQUEST_NOT_FOUND));
  }
}
