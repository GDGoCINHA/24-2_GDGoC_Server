package inha.gdgoc.domain.board.notice.repository;

import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import java.util.List;
import java.util.Optional;

public interface NoticeBoardRepository extends NoticeBoardQueryDslRepository {

  Optional<NoticeBoard> findById(Long id);

  Optional<NoticeBoard> findDeletedById(Long id);

  List<NoticeBoard> findAllByIdIn(List<Long> ids);

  NoticeBoard save(NoticeBoard notice);

  void increaseViewCount(Long id);
}
