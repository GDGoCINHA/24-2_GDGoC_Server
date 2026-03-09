package inha.gdgoc.domain.game.repository;

import inha.gdgoc.domain.game.entity.MbtiResult;
import inha.gdgoc.domain.game.repository.projection.MbtiTypeCountProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MbtiResultRepository extends JpaRepository<MbtiResult, Long> {
    Optional<MbtiResult> findByNameAndStudentId(String name, String studentId);

    Page<MbtiResult> findByNameContainingIgnoreCaseOrStudentIdContainingIgnoreCaseOrMbtiTypeContainingIgnoreCase(
            String nameKeyword,
            String studentIdKeyword,
            String mbtiTypeKeyword,
            Pageable pageable
    );

    List<MbtiResult> findByStudentIdIn(List<String> studentIds);

    @Query("select m.mbtiType as mbtiType, count(m) as count from MbtiResult m group by m.mbtiType")
    List<MbtiTypeCountProjection> countByMbtiType();
}
