package inha.gdgoc.domain.board.notice.repository;

import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeBoardJpaRepository extends JpaRepository<NoticeBoard, Long> {

  Optional<NoticeBoard> findByIdAndDeletedAtIsNull(Long id);

  Optional<NoticeBoard> findByIdAndDeletedAtIsNotNull(Long id);

  List<NoticeBoard> findAllByIdInAndDeletedAtIsNull(List<Long> ids);
}
