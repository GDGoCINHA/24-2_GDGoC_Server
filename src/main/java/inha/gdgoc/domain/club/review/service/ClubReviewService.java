package inha.gdgoc.domain.club.review.service;

import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.entity.ClubActivityPhoto;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.service.ClubActivityQueryService;
import inha.gdgoc.domain.club.completion.service.CompletionCalculator;
import inha.gdgoc.domain.club.review.dto.response.ClubReviewItemResponse;
import inha.gdgoc.domain.club.review.dto.response.ClubReviewItemResponse.Attendee;
import inha.gdgoc.domain.club.review.repository.ClubReviewQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영진의 활동 인증 검토. 상태 전이 규칙(인증 완료는 최종, 잠금)은 B 의 {@link ClubActivity} 가 갖고 있고 여기서는 부르기만 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubReviewService {

  private final ClubReviewQueryRepository clubReviewQueryRepository;
  private final ClubActivityQueryService clubActivityQueryService;

  /** @param status null 이면 PENDING */
  public List<ClubReviewItemResponse> findQueue(ClubActivityStatus status, Long clubId) {
    List<ClubActivity> activities =
        clubReviewQueryRepository.findForReview(
            status == null ? ClubActivityStatus.PENDING : status, clubId);
    if (activities.isEmpty()) {
      return List.of();
    }
    List<Long> ids = activities.stream().map(ClubActivity::getId).toList();

    Map<Long, List<String>> photos =
        clubReviewQueryRepository.findPhotos(ids).stream()
            .collect(
                Collectors.groupingBy(
                    p -> p.getActivity().getId(),
                    Collectors.mapping(ClubActivityPhoto::getUrl, Collectors.toList())));
    Map<Long, List<ClubActivityAttendance>> rosters =
        clubReviewQueryRepository.findRosters(ids).stream()
            .collect(Collectors.groupingBy(att -> att.getActivity().getId()));

    return activities.stream()
        .map(a -> toItem(a, photos.getOrDefault(a.getId(), List.of()), rosters.get(a.getId())))
        .toList();
  }

  @Transactional
  public void approve(Long activityId, Long reviewerId) {
    clubActivityQueryService.getForUpdate(activityId).approve(reviewerId, Instant.now());
  }

  @Transactional
  public void requestRevision(Long activityId, Long reviewerId, String reason) {
    clubActivityQueryService
        .getForUpdate(activityId)
        .requestRevision(reviewerId, reason.strip(), Instant.now());
  }

  private static ClubReviewItemResponse toItem(
      ClubActivity a, List<String> photoUrls, List<ClubActivityAttendance> rows) {
    List<Attendee> roster =
        rows == null
            ? List.of()
            : rows.stream()
                .map(r -> new Attendee(r.getUser().getId(), r.getUser().getName(), r.isAttended()))
                .toList();
    int attended = (int) roster.stream().filter(Attendee::attended).count();
    int required =
        CompletionCalculator.required(roster.size(), a.getClub().getTerm().getAttendanceRatio());
    return new ClubReviewItemResponse(
        a.getId(),
        a.getClub().getId(),
        a.getClub().getName(),
        a.getActivityDate(),
        a.getContent(),
        a.getProgressNote(),
        a.getStatus(),
        a.getSubmittedAt(),
        a.getStatus() == ClubActivityStatus.PENDING && a.getRevisionReason() != null,
        a.getRevisionReason(),
        photoUrls,
        roster,
        roster.size(),
        attended,
        required,
        CompletionCalculator.meetsRequired(roster.size(), attended, required));
  }
}
