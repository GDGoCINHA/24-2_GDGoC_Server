package inha.gdgoc.domain.board.notice.repository;

import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeBoardJpaRepository extends JpaRepository<NoticeBoard, Long> {

  Optional<NoticeBoard> findByIdAndDeletedAtIsNull(Long id);

  Optional<NoticeBoard> findByIdAndDeletedAtIsNotNull(Long id);

  List<NoticeBoard> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

  @Modifying
  @Query("UPDATE NoticeBoard n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
  void increaseViewCount(@Param("id") Long id);
}
