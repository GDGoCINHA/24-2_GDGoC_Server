package inha.gdgoc.domain.board.notice.repository;

import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import java.util.Optional;
import java.util.UUID;

public interface NoticeBoardRepository extends NoticeBoardQueryDslRepository {

    Optional<NoticeBoard> findById(UUID id);

    Optional<NoticeBoard> findDeletedById(UUID id);

    NoticeBoard save(NoticeBoard board);

    void delete(NoticeBoard board);

    void clearAllPinned();
}
