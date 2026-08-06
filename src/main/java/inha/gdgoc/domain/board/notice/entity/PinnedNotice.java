package inha.gdgoc.domain.board.notice.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상단 고정 슬롯. 최대 3행이며 display_order 는 1~3 중 하나로 유일하다.
 *
 * <p>개수 제한은 애플리케이션이 세지 않는다 — display_order 의 UNIQUE 와 CHECK(1~3)이 4행째를 물리적으로 막는다.
 */
@Entity
@Table(name = "pinned_notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PinnedNotice extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notice_board_id", nullable = false, unique = true)
  private NoticeBoard noticeBoard;

  @Column(nullable = false)
  private int displayOrder;

  @Column(name = "pinned_by", nullable = false)
  private Long pinnedBy;

  public static PinnedNotice create(NoticeBoard notice, int displayOrder, Long pinnedBy) {
    PinnedNotice pinned = new PinnedNotice();
    pinned.noticeBoard = notice;
    pinned.displayOrder = displayOrder;
    pinned.pinnedBy = pinnedBy;
    return pinned;
  }
}
