package inha.gdgoc.domain.club.leader.repository;

import inha.gdgoc.domain.club.leader.entity.ClubOpenRequest;
import inha.gdgoc.domain.club.leader.enums.ClubOpenRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubOpenRequestRepository extends JpaRepository<ClubOpenRequest, Long> {

  List<ClubOpenRequest> findAllByUserIdOrderByIdDesc(Long userId);

  @Query(
      "select r from ClubOpenRequest r join fetch r.user "
          + "where (:status is null or r.status = :status) order by r.id desc")
  List<ClubOpenRequest> findAllWithUser(@Param("status") ClubOpenRequestStatus status);
}
