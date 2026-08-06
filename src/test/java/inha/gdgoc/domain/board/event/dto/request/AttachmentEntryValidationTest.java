package inha.gdgoc.domain.board.event.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.board.event.dto.request.EventBoardCreateRequest.AttachmentEntry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** bean validation은 서비스 목 테스트를 거치지 않으므로, Validator를 직접 구동해 검증한다. */
class AttachmentEntryValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  @DisplayName("파일 첨부는 위반이 없다")
  void fileEntryHasNoViolations() {
    AttachmentEntry entry = new AttachmentEntry("user/1/event/a.pdf", "a.pdf", null);

    Set<ConstraintViolation<AttachmentEntry>> violations = validator.validate(entry);

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("https 링크 첨부는 위반이 없다")
  void httpsLinkEntryHasNoViolations() {
    AttachmentEntry entry = new AttachmentEntry(null, null, "https://example.com");

    Set<ConstraintViolation<AttachmentEntry>> violations = validator.validate(entry);

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("javascript 스킴 링크는 위반된다")
  void javascriptSchemeLinkIsRejected() {
    AttachmentEntry entry = new AttachmentEntry(null, null, "javascript:alert(1)");

    Set<ConstraintViolation<AttachmentEntry>> violations = validator.validate(entry);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @DisplayName("2048자를 넘는 url은 위반된다")
  void urlLongerThanMaxLengthIsRejected() {
    String url = "https://" + "a".repeat(2041); // 총 2049자
    AttachmentEntry entry = new AttachmentEntry(null, null, url);

    Set<ConstraintViolation<AttachmentEntry>> violations = validator.validate(entry);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @DisplayName("파일과 링크를 동시에 채우면 위반된다")
  void bothFileAndLinkPresentIsRejected() {
    AttachmentEntry entry = new AttachmentEntry("user/1/event/a.pdf", "a.pdf", "https://example.com");

    Set<ConstraintViolation<AttachmentEntry>> violations = validator.validate(entry);

    assertThat(violations).isNotEmpty();
  }

  @Test
  @DisplayName("셋 다 비어 있으면 위반된다")
  void allThreeNullIsRejected() {
    AttachmentEntry entry = new AttachmentEntry(null, null, null);

    Set<ConstraintViolation<AttachmentEntry>> violations = validator.validate(entry);

    assertThat(violations).isNotEmpty();
  }
}
