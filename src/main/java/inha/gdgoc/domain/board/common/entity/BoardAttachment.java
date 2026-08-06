package inha.gdgoc.domain.board.common.entity;

import inha.gdgoc.domain.board.common.enums.AttachmentKind;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 첨부의 공통 필드. 파일과 링크를 한 테이블에 담는다.
 *
 * <p>화면에서 파일과 링크가 한 목록에 순서대로 섞여 표시되므로 나누지 않는다. 종류별 배타성은 각 테이블의 CHECK 제약이
 * 지킨다.
 */
@Getter
@MappedSuperclass
public abstract class BoardAttachment extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AttachmentKind kind;

  @Column(length = 512)
  private String fileKey;

  @Column(length = 255)
  private String fileName;

  /** 서버가 S3 HeadObject 로 채운다. 클라이언트 값을 믿지 않는다. */
  @Column private Long fileSize;

  @Column(length = 2048)
  private String url;

  @Column(nullable = false)
  private int sortOrder;

  protected void initFile(String fileKey, String fileName, Long fileSize, int sortOrder) {
    this.kind = AttachmentKind.FILE;
    this.fileKey = fileKey;
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.sortOrder = sortOrder;
  }

  protected void initLink(String url, int sortOrder) {
    this.kind = AttachmentKind.LINK;
    this.url = url;
    this.sortOrder = sortOrder;
  }
}
