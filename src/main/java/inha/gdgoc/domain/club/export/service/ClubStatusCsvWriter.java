package inha.gdgoc.domain.club.export.service;

import inha.gdgoc.domain.club.export.dto.AdminClubRowResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 운영진 현황 표를 CSV 로. {@code ApplicantCsvWriter} 와 같이 UTF-8 BOM 을 넣는다 — 없으면 Excel 이 한글을 깨뜨린다.
 *
 * <p>팀 이름·리더 이름은 사용자가 쓴 값이라 {@code =}·{@code +} 등으로 시작하면 앞에 작은따옴표를 붙인다. 그대로 두면 Excel 이 수식으로
 * 실행한다(CSV 인젝션).
 */
@Component
public class ClubStatusCsvWriter {

  private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

  private static final List<String> HEADERS =
      List.of(
          "팀",
          "리더",
          "상태",
          "활동 시작",
          "활동 끝",
          "인원",
          "정원",
          "목표",
          "충족 주",
          "대상 주",
          "쉬는 주",
          "검토 대기",
          "경고",
          "기준 충족",
          "완주");

  public byte[] write(List<AdminClubRowResponse> rows) {
    StringBuilder sb = new StringBuilder();
    sb.append(HEADERS.stream().map(this::escape).collect(Collectors.joining(","))).append("\r\n");
    for (AdminClubRowResponse r : rows) {
      List<String> cells =
          List.of(
              r.name(),
              nz(r.leaderName()),
              r.status().name(),
              date(r.startDate()),
              date(r.endDate()),
              String.valueOf(r.memberCount()),
              r.capacity() == null ? "" : String.valueOf(r.capacity()),
              r.goalStatus().name(),
              String.valueOf(r.satisfiedWeeks()),
              String.valueOf(r.targetWeeks()),
              String.valueOf(r.restWeeks()),
              String.valueOf(r.pendingReviewCount()),
              r.warnings().stream().map(Enum::name).collect(Collectors.joining(" ")),
              r.eligible() ? "Y" : "N",
              r.completionStatus().name());
      sb.append(cells.stream().map(this::escape).collect(Collectors.joining(","))).append("\r\n");
    }
    return withBom(sb.toString());
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }

  private static String date(LocalDate d) {
    return d == null ? "" : d.toString();
  }

  String escape(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    String v = "=+-@\t\r".indexOf(raw.charAt(0)) >= 0 ? "'" + raw : raw;
    if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
      return '"' + v.replace("\"", "\"\"") + '"';
    }
    return v;
  }

  private static byte[] withBom(String body) {
    byte[] content = body.getBytes(StandardCharsets.UTF_8);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(UTF8_BOM.length + content.length)) {
      out.write(UTF8_BOM);
      out.write(content);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
