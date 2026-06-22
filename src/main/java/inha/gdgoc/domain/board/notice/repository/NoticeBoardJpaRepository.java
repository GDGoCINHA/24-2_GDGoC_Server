package inha.gdgoc.domain.board.notice.repository;

import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NoticeBoardJpaRepository extends JpaRepository<NoticeBoard, UUID> {

    Optional<NoticeBoard> findByArticleIdAndDeletedAtIsNull(UUID id);

    Optional<NoticeBoard> findByArticleIdAndDeletedAtIsNotNull(UUID id);
}
