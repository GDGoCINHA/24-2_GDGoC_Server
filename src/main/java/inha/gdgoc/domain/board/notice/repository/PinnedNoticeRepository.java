package inha.gdgoc.domain.board.notice.repository;

import inha.gdgoc.domain.board.notice.entity.PinnedNotice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PinnedNoticeRepository extends JpaRepository<PinnedNotice, Long> {

  @Query("SELECT p FROM PinnedNotice p JOIN FETCH p.noticeBoard ORDER BY p.displayOrder ASC")
  List<PinnedNotice> findAllOrdered();

  void deleteByNoticeBoardId(Long noticeBoardId);
}
