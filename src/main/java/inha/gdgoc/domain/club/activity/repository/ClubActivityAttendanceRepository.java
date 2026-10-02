package inha.gdgoc.domain.club.activity.repository;

import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubActivityAttendanceRepository
    extends JpaRepository<ClubActivityAttendance, Long> {

  @Query(
      "select a from ClubActivityAttendance a join fetch a.user "
          + "where a.activity.id = :activityId order by a.user.name asc, a.user.id asc")
  List<ClubActivityAttendance> findAllWithUser(@Param("activityId") Long activityId);

  /** 명단을 다시 만들기 전에 지운다. 같은 (활동, 사용자) 를 다시 넣으므로 먼저 DB 에서 지워져 있어야 한다. */
  @Modifying(flushAutomatically = true)
  @Query("delete from ClubActivityAttendance a where a.activity.id = :activityId")
  void deleteByActivityId(@Param("activityId") Long activityId);
}
