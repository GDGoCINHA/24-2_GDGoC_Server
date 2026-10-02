package inha.gdgoc.domain.club.schedule.service;

import inha.gdgoc.domain.club.activity.repository.ClubActivityRepository;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.service.ClubMemberQueryService;
import inha.gdgoc.domain.club.schedule.dto.request.ClubScheduleRequest;
import inha.gdgoc.domain.club.schedule.dto.response.ClubCheckinTokenResponse;
import inha.gdgoc.domain.club.schedule.dto.response.ClubScheduleResponse;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import inha.gdgoc.domain.club.schedule.entity.ClubScheduleRsvp;
import inha.gdgoc.domain.club.schedule.enums.ClubRsvp;
import inha.gdgoc.domain.club.schedule.exception.ClubScheduleErrorCode;
import inha.gdgoc.domain.club.schedule.repository.ClubScheduleCheckinRepository;
import inha.gdgoc.domain.club.schedule.repository.ClubScheduleRepository;
import inha.gdgoc.domain.club.schedule.repository.ClubScheduleRsvpRepository;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모임 일정과 참석 예정 응답. 일정 등록은 선택이다 — 일정 없이 모인 활동도 활동 기록으로 남길 수 있다.
 *
 * <p>참석 예정 응답은 실제 출석과 무관하다. 실제 출석은 리더가 활동 기록에 제출한 값이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final ClubAccessService clubAccessService;
  private final ClubMemberQueryService clubMemberQueryService;
  private final ClubScheduleRepository clubScheduleRepository;
  private final ClubScheduleRsvpRepository clubScheduleRsvpRepository;
  private final ClubScheduleCheckinRepository clubScheduleCheckinRepository;
  private final ClubActivityRepository clubActivityRepository;
  private final ClubCheckinTokenService clubCheckinTokenService;
  private final UserRepository userRepository;

  /**
   * 일정 목록. 부원이면 누구나 본다(기획 2.2 — 일정·출석 영역은 로그인 부원, 출석 명단만 팀·운영진).
   *
   * <p>오늘 시작한 일정은 하루 동안 다가오는 일정에 둔다 — 모임 중에 QR 을 띄워야 한다.
   *
   * <p>숨김 소모임은 운영진과 팀 멤버가 아니면 없는 것으로 본다(활동 기록 상세와 같은 규칙).
   */
  public List<ClubScheduleResponse> list(Long clubId, Long userId, boolean staff, boolean past) {
    Club club = clubAccessService.getClub(clubId);
    boolean member = clubAccessService.isActiveMember(clubId, userId);
    if (club.getStatus() == ClubStatus.HIDDEN && !staff && !member) {
      throw new BusinessException(ClubErrorCode.CLUB_NOT_FOUND);
    }

    LocalDate today = LocalDate.now(KST);
    Instant todayStart = today.atStartOfDay(KST).toInstant();
    List<ClubSchedule> schedules =
        past
            ? clubScheduleRepository.findAllByClubIdAndStartsAtLessThanOrderByStartsAtDesc(
                clubId, todayStart)
            : clubScheduleRepository.findAllByClubIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
                clubId, todayStart);
    if (schedules.isEmpty()) {
      return List.of();
    }

    // 집계는 지금 팀원만 센다. 탈퇴한 사람의 옛 응답이 남아 있어도 미응답 수가 음수가 되지 않는다.
    Set<Long> currentMembers =
        clubMemberQueryService.findActiveOn(clubId, today).stream()
            .map(m -> m.getUser().getId())
            .collect(Collectors.toSet());
    Map<Long, List<ClubScheduleRsvp>> rsvpsBySchedule =
        clubScheduleRsvpRepository
            .findAllByScheduleIds(schedules.stream().map(ClubSchedule::getId).toList())
            .stream()
            .filter(r -> currentMembers.contains(r.getUser().getId()))
            .collect(Collectors.groupingBy(r -> r.getSchedule().getId()));
    boolean insider = member || staff;

    return schedules.stream()
        .map(
            s -> {
              List<ClubScheduleRsvp> rsvps = rsvpsBySchedule.getOrDefault(s.getId(), List.of());
              long attend = rsvps.stream().filter(r -> r.getResponse() == ClubRsvp.ATTEND).count();
              long absent = rsvps.size() - attend;
              ClubRsvp mine =
                  rsvps.stream()
                      .filter(r -> r.getUser().getId().equals(userId))
                      .map(ClubScheduleRsvp::getResponse)
                      .findFirst()
                      .orElse(null);
              return new ClubScheduleResponse(
                  s.getId(),
                  s.getTitle(),
                  s.getStartsAt(),
                  s.getLocation(),
                  insider ? s.getOnlineLink() : null,
                  s.getDescription(),
                  attend,
                  absent,
                  Math.max(0, currentMembers.size() - rsvps.size()),
                  mine);
            })
        .toList();
  }

  @Transactional
  public Long create(Long clubId, Long userId, ClubScheduleRequest req) {
    Club club = clubAccessService.requireLeader(clubId, userId);
    ClubSchedule schedule =
        ClubSchedule.create(
            club,
            req.title().trim(),
            req.startsAt(),
            blankToNull(req.location()),
            blankToNull(req.onlineLink()),
            blankToNull(req.description()),
            userId);
    return clubScheduleRepository.save(schedule).getId();
  }

  @Transactional
  public void update(Long clubId, Long scheduleId, Long userId, ClubScheduleRequest req) {
    clubAccessService.requireLeader(clubId, userId);
    findInClub(clubId, scheduleId)
        .update(
            req.title().trim(),
            req.startsAt(),
            blankToNull(req.location()),
            blankToNull(req.onlineLink()),
            blankToNull(req.description()));
  }

  /** 일정을 지워도 활동 기록은 남는다. 연결만 끊겨 일정 없이 진행한 활동이 된다. 참석 응답과 QR 체크인은 함께 지운다. */
  @Transactional
  public void delete(Long clubId, Long scheduleId, Long userId) {
    clubAccessService.requireLeader(clubId, userId);
    ClubSchedule schedule = findInClub(clubId, scheduleId);
    clubActivityRepository.detachSchedule(scheduleId);
    clubScheduleRsvpRepository.deleteByScheduleId(scheduleId);
    clubScheduleCheckinRepository.deleteByScheduleId(scheduleId);
    clubScheduleRepository.delete(schedule);
  }

  /** 참석 예정 응답. 그 소모임의 지금 팀원만 한다. 다시 누르면 바꾼다. */
  @Transactional
  public void respond(Long scheduleId, Long userId, ClubRsvp response) {
    ClubSchedule schedule = find(scheduleId);
    clubAccessService.requireActiveMember(schedule.getClub().getId(), userId);
    clubScheduleRsvpRepository
        .findByScheduleIdAndUserId(scheduleId, userId)
        .ifPresentOrElse(
            rsvp -> rsvp.change(response),
            () ->
                clubScheduleRsvpRepository.save(
                    ClubScheduleRsvp.of(
                        schedule, userRepository.getReferenceById(userId), response)));
  }

  /** 리더 화면이 QR 로 띄울 토큰. 만료 전에 다시 불러 새로 그린다. */
  public ClubCheckinTokenResponse issueCheckinToken(Long scheduleId, Long userId) {
    ClubSchedule schedule = find(scheduleId);
    clubAccessService.requireLeader(schedule.getClub().getId(), userId);
    return new ClubCheckinTokenResponse(
        scheduleId,
        clubCheckinTokenService.issue(scheduleId),
        clubCheckinTokenService.lifetimeSeconds());
  }

  private ClubSchedule find(Long scheduleId) {
    return clubScheduleRepository
        .findById(scheduleId)
        .orElseThrow(() -> new BusinessException(ClubScheduleErrorCode.SCHEDULE_NOT_FOUND));
  }

  /** 다른 소모임의 일정을 경로만 바꿔 건드리지 못하게 한다. */
  private ClubSchedule findInClub(Long clubId, Long scheduleId) {
    ClubSchedule schedule = find(scheduleId);
    if (!schedule.getClub().getId().equals(clubId)) {
      throw new BusinessException(ClubScheduleErrorCode.SCHEDULE_NOT_FOUND);
    }
    return schedule;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
