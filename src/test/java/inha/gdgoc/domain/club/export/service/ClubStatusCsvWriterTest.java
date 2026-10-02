package inha.gdgoc.domain.club.export.service;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionStatus;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import inha.gdgoc.domain.club.export.dto.AdminClubRowResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubStatusCsvWriterTest {

  private final ClubStatusCsvWriter writer = new ClubStatusCsvWriter();

  private static AdminClubRowResponse row(String name) {
    return new AdminClubRowResponse(
        1L,
        name,
        10L,
        "리더",
        ClubStatus.ACTIVE,
        LocalDate.of(2026, 10, 5),
        null,
        4,
        null,
        ClubGoalStatus.SUBMITTED,
        2,
        8,
        1,
        3,
        List.of(ClubCompletionWarning.PERIOD_NOT_SET, ClubCompletionWarning.WEEK_MISSED),
        false,
        ClubCompletionStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("UTF-8 BOM 으로 시작하고 CRLF 로 줄을 나눈다")
  void bomAndCrlf() {
    byte[] out = writer.write(List.of(row("팀")));

    assertThat(Arrays.copyOf(out, 3)).containsExactly(0xEF, 0xBB, 0xBF);
    String body = new String(out, 3, out.length - 3, StandardCharsets.UTF_8);
    assertThat(body.split("\r\n")).hasSize(2);
    assertThat(body.split("\r\n")[1])
        .isEqualTo("팀,리더,ACTIVE,2026-10-05,,4,,SUBMITTED,2,8,1,3,PERIOD_NOT_SET WEEK_MISSED,N,IN_PROGRESS");
  }

  @Test
  @DisplayName("쉼표·따옴표는 감싸고, 수식으로 시작하는 값은 작은따옴표를 붙인다")
  void escapesAndNeutralizesFormulas() {
    assertThat(writer.escape("a,b")).isEqualTo("\"a,b\"");
    assertThat(writer.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    assertThat(writer.escape("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
    assertThat(writer.escape("+1")).isEqualTo("'+1");
    assertThat(writer.escape("@cmd")).isEqualTo("'@cmd");
    assertThat(writer.escape("일반")).isEqualTo("일반");
  }
}
