package inha.gdgoc.domain.club.review.repository;

import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.entity.ClubActivityPhoto;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 인증 검토 화면의 읽기 전용 조회. 활동 기록의 주인은 B 라 B 의 리포지토리를 늘리지 않고 C 쪽에 따로 둔다.
 *
 * <p>목록 한 번에 쿼리 세 번(기록·사진·명단)으로 끝낸다 — 기록마다 따로 읽으면 검토 대기가 쌓일수록 느려진다.
 */
public interface ClubReviewQueryRepository extends Repository<ClubActivity, Long> {

  /** 오래 기다린 것부터. {@code clubId} 가 null 이면 전체 팀. */
  @Query(
      "select a from ClubActivity a join fetch a.club c join fetch c.term "
          + "where a.status = :status and (:clubId is null or c.id = :clubId) "
          + "order by a.submittedAt asc, a.id asc")
  List<ClubActivity> findForReview(
      @Param("status") ClubActivityStatus status, @Param("clubId") Long clubId);

  @Query(
      "select p from ClubActivityPhoto p where p.activity.id in :activityIds "
          + "order by p.sortOrder asc, p.id asc")
  List<ClubActivityPhoto> findPhotos(@Param("activityIds") Collection<Long> activityIds);

  @Query(
      "select att from ClubActivityAttendance att join fetch att.user "
          + "where att.activity.id in :activityIds order by att.id asc")
  List<ClubActivityAttendance> findRosters(@Param("activityIds") Collection<Long> activityIds);
}
