package inha.gdgoc.domain.club.activity.repository;

import inha.gdgoc.domain.club.activity.entity.ClubAttendanceFixRequest;
import inha.gdgoc.domain.club.activity.enums.ClubFixRequestStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubAttendanceFixRequestRepository
    extends JpaRepository<ClubAttendanceFixRequest, Long> {

  /** 같은 기록·같은 사람의 대기 요청은 하나. 운영 DB 는 부분 유니크 인덱스로도 막지만 테스트 DB(H2)에는 없다. */
  boolean existsByActivityIdAndUserIdAndStatus(
      Long activityId, Long userId, ClubFixRequestStatus status);

  /** 리더의 처리 목록. 오래된 요청부터. */
  @Query(
      "select r from ClubAttendanceFixRequest r join fetch r.activity a join fetch r.user "
          + "where a.club.id = :clubId and r.status = :status order by r.createdAt asc, r.id asc")
  List<ClubAttendanceFixRequest> findAllByClubAndStatus(
      @Param("clubId") Long clubId, @Param("status") ClubFixRequestStatus status);

  /** 내 출석 목록에서 「요청 중」 을 표시할 기록. */
  @Query(
      "select r.activity.id from ClubAttendanceFixRequest r "
          + "where r.user.id = :userId and r.status = :status and r.activity.id in :activityIds")
  List<Long> findActivityIdsByUserAndStatus(
      @Param("userId") Long userId,
      @Param("status") ClubFixRequestStatus status,
      @Param("activityIds") Collection<Long> activityIds);
}
