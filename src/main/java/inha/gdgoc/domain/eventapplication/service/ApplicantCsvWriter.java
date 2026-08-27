package inha.gdgoc.domain.eventapplication.service;

import inha.gdgoc.domain.eventapplication.dto.response.ApplicantResponse;
import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.entity.QuestionOption;
import inha.gdgoc.global.util.MajorNormalizer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 신청자 목록을 CSV 로 만든다. 운영진은 결국 스프레드시트에서 확인하기 때문에 이게 없으면 구글폼을 계속 쓰게 된다.
 *
 * <p>맨 앞에 UTF-8 BOM 을 넣는다. 없으면 Excel 이 한글을 깨진 글자로 연다 — 파일 자체는 멀쩡한데 받는 사람에게는 버그로 보인다.
 */
@Component
public class ApplicantCsvWriter {

  private final MajorNormalizer majorNormalizer;

  public ApplicantCsvWriter(MajorNormalizer majorNormalizer) {
    this.majorNormalizer = majorNormalizer;
  }

  private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(KST);

  private static final List<String> FIXED_HEADERS =
      List.of("이름", "학번", "학과", "이메일", "연락처", "신청일시", "상태", "참석", "체크인");

  /**
   * @param questions 삭제된 질문까지 포함해 넘긴다. 지운 뒤에도 과거 답변은 남아 있기 때문이다
   */
  public byte[] write(List<EventFormQuestion> questions, List<ApplicantResponse> applicants) {
    StringBuilder sb = new StringBuilder();

    List<String> headers = new java.util.ArrayList<>(FIXED_HEADERS);
    for (EventFormQuestion question : questions) {
      headers.add(question.isDeleted() ? "(삭제됨) " + question.getLabel() : question.getLabel());
    }
    sb.append(headers.stream().map(this::escape).collect(Collectors.joining(","))).append("\r\n");

    for (ApplicantResponse applicant : applicants) {
      List<String> cells = new java.util.ArrayList<>();
      cells.add(applicant.name());
      cells.add(applicant.studentId());
      // 저장된 값은 'ME' 같은 코드다. 스프레드시트를 여는 사람에게는 학과명이어야 한다.
      cells.add(majorNormalizer.toLabel(applicant.major()));
      cells.add(applicant.email());
      cells.add(applicant.phoneNumber());
      cells.add(format(applicant.appliedAt()));
      cells.add(applicant.status() == null ? "" : applicant.status().name());
      cells.add(applicant.attendanceStatus() == null ? "" : applicant.attendanceStatus().getLabel());
      cells.add(format(applicant.checkedInAt()));

      Map<Long, Object> answers = applicant.answers();
      for (EventFormQuestion question : questions) {
        cells.add(
            stringify(answers == null ? null : answers.get(question.getId()), labelsOf(question)));
      }
      sb.append(cells.stream().map(this::escape).collect(Collectors.joining(","))).append("\r\n");
    }

    return withBom(sb.toString());
  }

  private byte[] withBom(String body) {
    byte[] content = body.getBytes(StandardCharsets.UTF_8);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(UTF8_BOM.length + content.length)) {
      out.write(UTF8_BOM);
      out.write(content);
      return out.toByteArray();
    } catch (IOException e) {
      // ByteArrayOutputStream 은 IO 를 하지 않는다. 여기 오면 JDK 가 이상한 것이다.
      throw new IllegalStateException(e);
    }
  }

  private String format(Instant instant) {
    return instant == null ? "" : TIMESTAMP.format(instant);
  }

  /** 답변에는 선택지의 value 가 들어 있다. 사람이 읽는 파일이므로 label 로 바꿔 적는다. */
  private Map<String, String> labelsOf(EventFormQuestion question) {
    if (question.getOptions() == null) {
      return Map.of();
    }
    Map<String, String> labels = new java.util.HashMap<>();
    for (QuestionOption option : question.getOptions()) {
      if (option != null && option.value() != null && option.label() != null) {
        labels.put(option.value(), option.label());
      }
    }
    return labels;
  }

  /** 다중선택은 한 칸에 모아 적는다. 셀 안의 쉼표는 escape 가 처리한다. */
  private String stringify(Object value, Map<String, String> labels) {
    if (value == null) {
      return "";
    }
    if (value instanceof Collection<?> collection) {
      return collection.stream()
          .map(item -> label(item, labels))
          .collect(Collectors.joining(", "));
    }
    if (value instanceof Boolean b) {
      return b ? "O" : "X";
    }
    return label(value, labels);
  }

  /** 선택지가 지워졌거나 자유 입력이면 저장된 값을 그대로 쓴다. */
  private String label(Object value, Map<String, String> labels) {
    String raw = String.valueOf(value);
    return labels.getOrDefault(raw, raw);
  }

  private String escape(String raw) {
    if (raw == null) {
      return "";
    }
    if (raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r")) {
      return '"' + raw.replace("\"", "\"\"") + '"';
    }
    return raw;
  }
}
