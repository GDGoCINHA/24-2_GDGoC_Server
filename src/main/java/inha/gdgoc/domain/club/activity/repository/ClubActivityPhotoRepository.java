package inha.gdgoc.domain.club.activity.repository;

import inha.gdgoc.domain.club.activity.entity.ClubActivityPhoto;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubActivityPhotoRepository extends JpaRepository<ClubActivityPhoto, Long> {

  List<ClubActivityPhoto> findAllByActivityIdOrderBySortOrderAsc(Long activityId);

  /** 피드 한 페이지의 사진을 한 번에. */
  @Query(
      "select p from ClubActivityPhoto p where p.activity.id in :activityIds "
          + "order by p.activity.id, p.sortOrder")
  List<ClubActivityPhoto> findAllByActivityIds(
      @Param("activityIds") Collection<Long> activityIds);

  @Modifying(flushAutomatically = true)
  @Query("delete from ClubActivityPhoto p where p.activity.id = :activityId")
  void deleteByActivityId(@Param("activityId") Long activityId);
}
