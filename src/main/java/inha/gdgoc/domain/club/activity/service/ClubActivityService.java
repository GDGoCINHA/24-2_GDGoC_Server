package inha.gdgoc.domain.club.activity.service;

import inha.gdgoc.domain.club.activity.dto.request.ClubActivitySubmitRequest;
import inha.gdgoc.domain.club.activity.dto.response.ClubActivityDetailResponse;
import inha.gdgoc.domain.club.activity.dto.response.ClubActivityRosterResponse;
import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.entity.ClubActivityPhoto;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.domain.club.activity.repository.ClubActivityAttendanceRepository;
import inha.gdgoc.domain.club.activity.repository.ClubActivityPhotoRepository;
import inha.gdgoc.domain.club.activity.repository.ClubActivityRepository;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.service.ClubMemberQueryService;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import inha.gdgoc.domain.club.schedule.repository.ClubScheduleCheckinRepository;
import inha.gdgoc.domain.club.schedule.repository.ClubScheduleRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 활동 기록(인증) 제출·수정·조회.
 *
 * <p><b>출석 명단은 서버가 정한다.</b> 요청은 출석한 사람만 보내고, 서버가 활동일 당시 명단 전원을 출석 행으로 넣는다. 행 수가 참석 비율의
 * 분모이므로 화면이 명단을 고를 수 있으면 분모를 줄여 비율을 부풀릴 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubActivityService {

  private static final Comparator<User> BY_NAME =
      Comparator.comparing(User::getName, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(User::getId);

  private final ClubAccessService clubAccessService;
  private final ClubMemberQueryService clubMemberQueryService;
  private final ClubActivityQueryService clubActivityQueryService;
  private final ClubActivityRepository clubActivityRepository;
  private final ClubActivityPhotoRepository clubActivityPhotoRepository;
  private final ClubActivityAttendanceRepository clubActivityAttendanceRepository;
  private final ClubScheduleRepository clubScheduleRepository;
  private final ClubScheduleCheckinRepository clubScheduleCheckinRepository;

  /** 작성 화면의 명단. 일정을 주면 QR 체크인 여부를 함께 준다. */
  public ClubActivityRosterResponse roster(
      Long clubId, Long userId, LocalDate date, Long scheduleId) {
    Club club = clubAccessService.requireLeader(clubId, userId);
    Set<Long> checkedIn =
        scheduleId == null
            ? Set.of()
            : new HashSet<>(
                clubScheduleCheckinRepository.findUserIdsByScheduleId(
                    findSchedule(club, scheduleId).getId()));
    List<User> roster = rosterOn(clubId, date);
    BigDecimal ratio = club.getTerm().getAttendanceRatio();
    Long leaderId = club.getLeader().getId();
    return new ClubActivityRosterResponse(
        date,
        ratio,
        RequiredAttendance.of(roster.size(), ratio),
        roster.stream()
            .map(
                u ->
                    new ClubActivityRosterResponse.Member(
                        u.getId(), u.getName(), u.getId().equals(leaderId), checkedIn.contains(u.getId())))
            .toList());
  }

  @Transactional
  public Long create(Long clubId, Long userId, ClubActivitySubmitRequest req) {
    Club club = clubAccessService.requireLeader(clubId, userId);
    ClubSchedule schedule = resolveSchedule(club, req.scheduleId(), null);
    ClubActivity activity =
        ClubActivity.create(
            club,
            schedule,
            req.activityDate(),
            req.content(),
            req.progressNote(),
            userId,
            Instant.now());
    clubActivityRepository.save(activity);
    flushScheduleLink();
    replacePhotos(activity, req.photoUrls());
    replaceAttendance(activity, req.attendedUserIds());
    return activity.getId();
  }

  /**
   * 확인 중·보완 요청 상태만 고칠 수 있고, 고치면 확인 중으로 돌아간다. 인증 완료면 409.
   *
   * <p>운영진 검토와 동시에 들어와도 한쪽만 이기도록 잠가서 읽는다. 명단은 활동일이 그대로여도 매번 다시 만든다.
   */
  @Transactional
  public void update(Long clubId, Long activityId, Long userId, ClubActivitySubmitRequest req) {
    Club club = clubAccessService.requireLeader(clubId, userId);
    ClubActivity activity = clubActivityQueryService.getForUpdate(activityId);
    requireInClub(activity, clubId);
    activity.requireEditable();
    ClubSchedule schedule = resolveSchedule(club, req.scheduleId(), activityId);
    activity.update(
        schedule, req.activityDate(), req.content(), req.progressNote(), Instant.now());
    flushScheduleLink();
    replacePhotos(activity, req.photoUrls());
    replaceAttendance(activity, req.attendedUserIds());
  }

  /**
   * 상세. 출석 명단과 보완 사유는 팀 멤버와 운영진에게만 준다.
   *
   * <p>숨김(HIDDEN) 소모임은 운영진과 그 팀 멤버가 아니면 없는 것으로 본다 — A 의 소모임 상세({@code ClubService#getDetail})와 같은
   * 규칙이다.
   */
  public ClubActivityDetailResponse detail(
      Long clubId, Long activityId, Long userId, boolean staff) {
    Club club = clubAccessService.getClub(clubId);
    boolean member = clubAccessService.isActiveMember(clubId, userId);
    if (club.getStatus() == ClubStatus.HIDDEN && !staff && !member) {
      throw new BusinessException(ClubErrorCode.CLUB_NOT_FOUND);
    }
    ClubActivity activity =
        clubActivityRepository
            .findById(activityId)
            .orElseThrow(() -> new BusinessException(ClubActivityErrorCode.ACTIVITY_NOT_FOUND));
    requireInClub(activity, clubId);

    List<ClubActivityAttendance> rows = clubActivityAttendanceRepository.findAllWithUser(activityId);
    long attended = rows.stream().filter(ClubActivityAttendance::isAttended).count();
    boolean insider = member || staff;
    boolean leader = userId != null && userId.equals(club.getLeader().getId());

    return new ClubActivityDetailResponse(
        activity.getId(),
        clubId,
        activity.getSchedule() == null ? null : activity.getSchedule().getId(),
        activity.getActivityDate(),
        activity.getContent(),
        activity.getProgressNote(),
        activity.getStatus(),
        clubActivityPhotoRepository.findAllByActivityIdOrderBySortOrderAsc(activityId).stream()
            .map(ClubActivityPhoto::getUrl)
            .toList(),
        rows.size(),
        attended,
        RequiredAttendance.of(rows.size(), club.getTerm().getAttendanceRatio()),
        insider
            ? rows.stream()
                .map(
                    r ->
                        new ClubActivityDetailResponse.Attendance(
                            r.getUser().getId(), r.getUser().getName(), r.isAttended()))
                .toList()
            : null,
        insider ? activity.getRevisionReason() : null,
        activity.getSubmittedAt(),
        leader && !activity.isApproved());
  }

  /**
   * 활동일 당시 명단. 재가입은 새 멤버 행이라 한 사람이 행을 여러 개 가질 수 있으므로 사람 단위로 한 번 더 거른다 — 겹치면 출석 행의 유니크
   * 제약이 터진다.
   */
  private List<User> rosterOn(Long clubId, LocalDate date) {
    Map<Long, User> byId = new LinkedHashMap<>();
    for (ClubMember m : clubMemberQueryService.findActiveOn(clubId, date)) {
      byId.putIfAbsent(m.getUser().getId(), m.getUser());
    }
    return byId.values().stream().sorted(BY_NAME).toList();
  }

  /** 명단 전원을 넣고, 요청에 있는 사람만 출석으로 표시한다. 명단 밖 id 는 무시한다. */
  private void replaceAttendance(ClubActivity activity, Collection<Long> attendedUserIds) {
    Set<Long> attended = attendedUserIds == null ? Set.of() : new HashSet<>(attendedUserIds);
    clubActivityAttendanceRepository.deleteByActivityId(activity.getId());
    clubActivityAttendanceRepository.saveAll(
        rosterOn(activity.getClub().getId(), activity.getActivityDate()).stream()
            .map(u -> ClubActivityAttendance.of(activity, u, attended.contains(u.getId())))
            .toList());
  }

  private void replacePhotos(ClubActivity activity, List<String> urls) {
    clubActivityPhotoRepository.deleteByActivityId(activity.getId());
    List<ClubActivityPhoto> photos = new ArrayList<>();
    for (int i = 0; i < urls.size(); i++) {
      photos.add(ClubActivityPhoto.of(activity, urls.get(i), i));
    }
    clubActivityPhotoRepository.saveAll(photos);
  }

  /**
   * 연결할 일정. 같은 소모임의 일정이어야 하고, 다른 기록이 이미 쓰고 있으면 409.
   *
   * <p>일정 날짜와 활동일이 달라도 막지 않는다 — 미뤄진 모임을 원래 일정에 연결하는 경우가 있다.
   */
  private ClubSchedule resolveSchedule(Club club, Long scheduleId, Long editingActivityId) {
    if (scheduleId == null) {
      return null;
    }
    ClubSchedule schedule = findSchedule(club, scheduleId);
    boolean taken =
        editingActivityId == null
            ? clubActivityRepository.existsByScheduleId(scheduleId)
            : clubActivityRepository.existsByScheduleIdAndIdNot(scheduleId, editingActivityId);
    if (taken) {
      throw new BusinessException(ClubActivityErrorCode.SCHEDULE_ALREADY_RECORDED);
    }
    return schedule;
  }

  private ClubSchedule findSchedule(Club club, Long scheduleId) {
    return clubScheduleRepository
        .findById(scheduleId)
        .filter(s -> s.getClub().getId().equals(club.getId()))
        .orElseThrow(() -> new BusinessException(ClubActivityErrorCode.SCHEDULE_NOT_FOUND));
  }

  /** 위의 중복 검사를 지나 동시에 같은 일정에 두 기록이 들어오면 운영 DB 의 유니크 제약이 잡는다. 그걸 500 이 아닌 409 로 바꾼다. */
  private void flushScheduleLink() {
    try {
      clubActivityRepository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(ClubActivityErrorCode.SCHEDULE_ALREADY_RECORDED);
    }
  }

  /** 다른 소모임의 기록을 경로만 바꿔 건드리지 못하게 한다. 있다는 것도 알려주지 않으려고 404 다. */
  private void requireInClub(ClubActivity activity, Long clubId) {
    if (!activity.getClub().getId().equals(clubId)) {
      throw new BusinessException(ClubActivityErrorCode.ACTIVITY_NOT_FOUND);
    }
  }
}
