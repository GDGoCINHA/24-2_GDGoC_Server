package inha.gdgoc.domain.board.common.enums;

/**
 * 값을 추가하면 이 enum을 쓰는 각 게시판 테이블의 CHECK 제약(예: {@code ck_event_board_attachment_kind})도 함께
 * ALTER 해야 한다. enum과 제약이 서로 다른 곳에 있어 코드도 테스트도 이 불일치를 잡아주지 않는다.
 */
public enum AttachmentKind {
  FILE,
  LINK
}
