package inha.gdgoc.domain.board.event.dto.request;

import inha.gdgoc.domain.user.enums.TeamType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record EventBoardCreateRequest(
    @NotBlank String title,
    @NotNull LocalDate eventStartDate,
    @NotNull LocalDate eventEndDate,
    @NotNull TeamType organizingTeam,
    String thumbnailKey,
    @NotBlank String content,
    boolean isPublished,
    @Valid List<AttachmentEntry> attachments) {

  @AssertTrue(message = "행사 종료일은 시작일보다 앞설 수 없습니다.")
  private boolean isEventPeriodValid() {
    return eventStartDate == null || eventEndDate == null || !eventEndDate.isBefore(eventStartDate);
  }

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
}
