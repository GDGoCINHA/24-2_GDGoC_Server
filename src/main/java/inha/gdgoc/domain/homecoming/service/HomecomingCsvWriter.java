package inha.gdgoc.domain.homecoming.service;

import inha.gdgoc.domain.homecoming.entity.HomecomingApplication;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 명찰·전체 명단 CSV. 운영진은 스프레드시트와 Notion 에 옮겨 쓴다.
 *
 * <p>맨 앞에 UTF-8 BOM 을 넣는다. 없으면 Excel 이 한글을 깨뜨린다. 셀이 {@code = + - @} 로 시작하면 앞에 작은따옴표를 붙여 수식으로
 * 실행되지 않게 한다 — 이름·요청사항은 누구나 적을 수 있는 칸이다.
 */
@Component
public class HomecomingCsvWriter {

  private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));

  /** 명찰용. 본 행사 참석자(main·both)만 넘긴다. */
  public byte[] nametags(List<HomecomingApplication> rows) {
    List<List<String>> lines = new ArrayList<>();
    lines.add(List.of("이름", "명찰 표기", "시작 연도", "마지막 연도", "활동 구분", "활동 팀", "참석 유형"));
    for (HomecomingApplication a : rows) {
      lines.add(
          List.of(
              a.getName(),
              a.activitySummary(),
              str(a.getYearStart()),
              str(a.getYearEnd()),
              a.getMemberRole() == null ? "" : a.getMemberRole().getLabel(),
              a.getTeam() == null ? "" : a.getTeam().getLabel(),
              a.getAttendType().getLabel()));
    }
    return encode(lines);
  }

  /** 전체 명단. HR 이 Notion 명단과 이름·전화번호로 대조할 때 쓴다. 취소 건도 상태와 함께 넣는다. */
  public byte[] applications(List<HomecomingApplication> rows) {
    List<List<String>> lines = new ArrayList<>();
    lines.add(
        List.of(
            "신청일시", "상태", "참석 유형", "Notion 표시", "이름", "전화번호", "활동", "늦참", "도착 예정", "문의·요청사항", "취소 사유"));
    for (HomecomingApplication a : rows) {
      lines.add(
          List.of(
              TIMESTAMP.format(a.getSubmittedAt()),
              a.isActive() ? "신청" : "취소",
              a.getAttendType().getLabel(),
              a.getAttendType().getNotionStatus(),
              a.getName(),
              a.getPhone(),
              a.activitySummary(),
              a.isLate() ? "Y" : "",
              a.getArrivalTime() == null ? "" : a.getArrivalTime(),
              a.getMessage() == null ? "" : a.getMessage(),
              a.getCancelReason() == null ? "" : a.getCancelReason()));
    }
    return encode(lines);
  }

  private byte[] encode(List<List<String>> lines) {
    String body =
        lines.stream()
            .map(cells -> cells.stream().map(HomecomingCsvWriter::escape).collect(Collectors.joining(",")))
            .collect(Collectors.joining("\r\n", "", "\r\n"));
    byte[] text = body.getBytes(StandardCharsets.UTF_8);
    byte[] out = new byte[UTF8_BOM.length + text.length];
    System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
    System.arraycopy(text, 0, out, UTF8_BOM.length, text.length);
    return out;
  }

  static String escape(String raw) {
    String v = raw == null ? "" : raw;
    if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) v = "'" + v;
    if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
      v = "\"" + v.replace("\"", "\"\"") + "\"";
    }
    return v;
  }

  private static String str(Integer v) {
    return v == null ? "" : v.toString();
  }
}
