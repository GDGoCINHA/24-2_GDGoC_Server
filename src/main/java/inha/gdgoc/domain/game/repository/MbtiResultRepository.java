package inha.gdgoc.domain.game.repository;

import inha.gdgoc.domain.game.entity.MbtiResult;
import inha.gdgoc.domain.game.repository.projection.MbtiTypeCountProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MbtiResultRepository extends JpaRepository<MbtiResult, Long> {
    Optional<MbtiResult> findByNameAndStudentId(String name, String studentId);

    @Query("select m.mbtiType as mbtiType, count(m) as count from MbtiResult m group by m.mbtiType")
    List<MbtiTypeCountProjection> countByMbtiType();
}
