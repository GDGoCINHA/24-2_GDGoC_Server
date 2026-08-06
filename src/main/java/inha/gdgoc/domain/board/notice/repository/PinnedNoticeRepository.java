package inha.gdgoc.domain.board.notice.repository;

import inha.gdgoc.domain.board.notice.entity.PinnedNotice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PinnedNoticeRepository extends JpaRepository<PinnedNotice, Long> {

  /**
   * 비공개로 전환되거나 삭제된 공지는 고정 슬롯에 남아 있어도 목록에 노출되면 안 된다. deleteNotice 는 pinned_notice 행을
   * 직접 지우지만, 비공개 전환(PATCH)에는 그런 정리 로직이 없다 — 이 필터가 그 경로를 포함한 모든 상태 변화를 방어하는
   * 마지막 수단이므로 "단순화"한다고 지우면 안 된다.
   */
  @Query(
      "SELECT p FROM PinnedNotice p JOIN FETCH p.noticeBoard n "
          + "WHERE n.deletedAt IS NULL AND n.isPublished = true "
          + "ORDER BY p.displayOrder ASC")
  List<PinnedNotice> findAllOrdered();

  void deleteByNoticeBoardId(Long noticeBoardId);
}
