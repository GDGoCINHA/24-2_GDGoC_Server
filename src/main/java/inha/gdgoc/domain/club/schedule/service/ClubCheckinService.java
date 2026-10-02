package inha.gdgoc.domain.club.schedule.service;

import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.schedule.dto.response.ClubCheckinResponse;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import inha.gdgoc.domain.club.schedule.entity.ClubScheduleCheckin;
import inha.gdgoc.domain.club.schedule.exception.ClubScheduleErrorCode;
import inha.gdgoc.domain.club.schedule.repository.ClubScheduleCheckinRepository;
import inha.gdgoc.domain.club.schedule.repository.ClubScheduleRepository;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * QR 체크인. 리더가 모임에서 QR 을 띄우고 멤버가 각자 찍는다.
 *
 * <p>체크인 기록은 활동 기록 작성 화면에서 출석 체크의 <b>기본값</b>으로만 쓰인다. 최종 출석은 리더가 제출한 값이다.
 */
@Service
@RequiredArgsConstructor
public class ClubCheckinService {

  private final ClubCheckinTokenService clubCheckinTokenService;
  private final ClubScheduleRepository clubScheduleRepository;
  private final ClubScheduleCheckinRepository clubScheduleCheckinRepository;
  private final ClubAccessService clubAccessService;
  private final UserRepository userRepository;

  /**
   * 이미 체크인했으면 그대로 200 이다. 두 번 찍는 것은 흔한 일이다.
   *
   * <p><b>트랜잭션 밖에서 돈다.</b> 같은 사람의 요청 두 개가 동시에 들어오면 늦은 쪽의 저장이 유니크 제약에 걸린다. 그 예외를 트랜잭션 안에서
   * 잡으면 트랜잭션이 롤백 전용으로 표시돼 응답은 200 인데 커밋에서 500 이 난다. 저장만 리포지토리의 자체 트랜잭션으로 돌려, 걸리면 먼저 들어간
   * 행을 읽어 "이미 체크인" 으로 답한다.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public ClubCheckinResponse checkIn(String token, Long userId) {
    Long scheduleId =
        clubCheckinTokenService
            .verify(token)
            .orElseThrow(() -> new BusinessException(ClubScheduleErrorCode.CHECKIN_TOKEN_INVALID));
    ClubSchedule schedule =
        clubScheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new BusinessException(ClubScheduleErrorCode.SCHEDULE_NOT_FOUND));
    Long clubId = schedule.getClub().getId();
    clubAccessService.requireActiveMember(clubId, userId);

    Optional<ClubScheduleCheckin> existing =
        clubScheduleCheckinRepository.findByScheduleIdAndUserId(scheduleId, userId);
    if (existing.isPresent()) {
      return already(clubId, schedule, existing.get());
    }

    // DB(timestamptz)는 마이크로초까지만 담는다. 잘라 두지 않으면 첫 응답의 시각과 다시 찍었을 때 읽어 온 시각이 어긋난다.
    Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    try {
      clubScheduleCheckinRepository.save(
          ClubScheduleCheckin.of(schedule, userRepository.getReferenceById(userId), now));
    } catch (DataIntegrityViolationException e) {
      return clubScheduleCheckinRepository
          .findByScheduleIdAndUserId(scheduleId, userId)
          .map(first -> already(clubId, schedule, first))
          .orElseThrow(() -> e);
    }
    return new ClubCheckinResponse(clubId, scheduleId, schedule.getTitle(), false, now);
  }

  private ClubCheckinResponse already(
      Long clubId, ClubSchedule schedule, ClubScheduleCheckin checkin) {
    return new ClubCheckinResponse(
        clubId, schedule.getId(), schedule.getTitle(), true, checkin.getCheckedAt());
  }
}
