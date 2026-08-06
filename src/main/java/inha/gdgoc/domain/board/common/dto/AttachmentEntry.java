package inha.gdgoc.domain.board.common.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/** 첨부 요청 한 건. 파일(fileKey+fileName)이거나 링크(url)이며 둘을 겸할 수 없다. */
public record AttachmentEntry(
    @Size(max = 512) String fileKey,
    @Size(max = 255) String fileName,
    @Size(max = 2048) String url) {

  @AssertTrue(
      message = "첨부는 파일(fileKey·fileName) 또는 링크(url) 중 하나여야 하며, 링크는 http:// 또는 https:// 로 시작해야 합니다.")
  private boolean isExactlyOneKind() {
    boolean isFile = fileKey != null && !fileKey.isBlank() && fileName != null && !fileName.isBlank();
    boolean isLink =
        url != null
            && !url.isBlank()
            && (url.startsWith("http://") || url.startsWith("https://"));
    return isFile ^ isLink;
  }
}
