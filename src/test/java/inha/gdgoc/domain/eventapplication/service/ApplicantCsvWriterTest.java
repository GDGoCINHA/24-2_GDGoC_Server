package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.global.util.MajorNormalizer;
import inha.gdgoc.domain.eventapplication.dto.response.ApplicantResponse;
import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.entity.QuestionOption;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.enums.EventAttendanceStatus;
import inha.gdgoc.domain.eventapplication.enums.QuestionType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ApplicantCsvWriterTest {

  private final ApplicantCsvWriter writer = new ApplicantCsvWriter(new MajorNormalizer());

  @Test
  @DisplayName("UTF-8 BOM 으로 시작한다")
  void startsWithBom() {
    byte[] csv = writer.write(List.of(), List.of());

    // BOM 이 없으면 Excel 이 한글을 깨서 연다. 파일은 멀쩡한데 받는 사람에게는 버그로 보인다.
    assertThat(csv[0]).isEqualTo((byte) 0xEF);
    assertThat(csv[1]).isEqualTo((byte) 0xBB);
    assertThat(csv[2]).isEqualTo((byte) 0xBF);
  }

  @Test
  @DisplayName("질문이 헤더 뒤쪽 열이 된다")
  void questionsBecomeColumns() {
    EventFormQuestion q1 = question(1L, "저녁 참여", QuestionType.SINGLE_CHOICE);
    EventFormQuestion q2 = question(2L, "티셔츠 사이즈", QuestionType.DROPDOWN);

    String csv = text(writer.write(List.of(q1, q2), List.of()));

    assertThat(firstLine(csv)).isEqualTo("이름,학번,학과,이메일,연락처,신청일시,상태,참석,체크인,저녁 참여,티셔츠 사이즈");
  }

  @Test
  @DisplayName("삭제된 질문도 표시를 달아 열로 남긴다")
  void deletedQuestionsStayAsColumns() {
    EventFormQuestion q = question(1L, "예전 질문", QuestionType.SHORT_TEXT);
    q.softDelete();

    String csv = text(writer.write(List.of(q), List.of()));

    // 질문을 지워도 과거 답변은 남아 있으므로 열이 사라지면 데이터를 잃는다.
    assertThat(firstLine(csv)).endsWith(",(삭제됨) 예전 질문");
  }

  @Test
  @DisplayName("쉼표·따옴표·줄바꿈이 든 답변을 감싼다")
  void escapesSpecialCharacters() {
    EventFormQuestion q = question(1L, "하고 싶은 말", QuestionType.LONG_TEXT);
    String csv =
        text(writer.write(List.of(q), List.of(applicant(answers(1L, "안녕, \"반가워\"\n잘 부탁해")))));

    assertThat(csv).contains("\"안녕, \"\"반가워\"\"\n잘 부탁해\"");
  }

  @Test
  @DisplayName("다중선택은 한 칸에 모아 적는다")
  void joinsMultiChoice() {
    EventFormQuestion q = question(1L, "관심 세션", QuestionType.MULTI_CHOICE);
    String csv = text(writer.write(List.of(q), List.of(applicant(answers(1L, List.of("A", "C"))))));

    // 셀 안에 쉼표가 생기므로 따옴표로 감싸져야 한다.
    assertThat(csv).contains("\"A, C\"");
  }

  @Test
  @DisplayName("동의 답변은 O/X 로 적는다")
  void booleanBecomesMark() {
    EventFormQuestion q = question(1L, "개인정보 동의", QuestionType.AGREEMENT);

    assertThat(text(writer.write(List.of(q), List.of(applicant(answers(1L, true)))))).contains(",O");
    assertThat(text(writer.write(List.of(q), List.of(applicant(answers(1L, false)))))).contains(",X");
  }

  @Test
  @DisplayName("답하지 않은 질문은 빈 칸으로 둔다")
  void missingAnswerBecomesEmptyCell() {
    EventFormQuestion q = question(1L, "선택 질문", QuestionType.SHORT_TEXT);
    String csv = text(writer.write(List.of(q), List.of(applicant(new LinkedHashMap<>()))));

    assertThat(csv.strip()).endsWith(",");
  }

  @Test
  @DisplayName("선택형 답변을 선택지 문구로 바꿔 적는다")
  void optionValueBecomesLabel() {
    // 폼 빌더가 만드는 value 는 OPT_1756... 같은 내부 값이다. 그대로 적으면 아무도 못 읽는다.
    EventFormQuestion q =
        choiceQuestion(
            1L,
            "식사 여부",
            QuestionType.SINGLE_CHOICE,
            List.of(new QuestionOption("OPT_1", "네, 먹습니다"), new QuestionOption("OPT_2", "아니요")));

    String csv = text(writer.write(List.of(q), List.of(applicant(answers(1L, "OPT_1")))));

    assertThat(csv).contains("\"네, 먹습니다\"").doesNotContain("OPT_1");
  }

  @Test
  @DisplayName("다중선택도 선택지 문구로 바꿔 모아 적는다")
  void multiChoiceValuesBecomeLabels() {
    EventFormQuestion q =
        choiceQuestion(
            1L,
            "관심 세션",
            QuestionType.MULTI_CHOICE,
            List.of(new QuestionOption("OPT_1", "서버리스"), new QuestionOption("OPT_2", "배포")));

    String csv =
        text(writer.write(List.of(q), List.of(applicant(answers(1L, List.of("OPT_1", "OPT_2"))))));

    assertThat(csv).contains("\"서버리스, 배포\"");
  }

  // 스프레드시트를 여는 사람에게 'CSE' 는 아무 뜻이 없다. 화면 표는 예전부터 라벨로 보여 줬는데
  // CSV 만 코드가 나갔다.
  @Test
  @DisplayName("학과는 코드가 아니라 학과명으로 적는다")
  void majorCodeBecomesLabel() {
    String csv = text(writer.write(List.of(), List.of(applicant(Map.of()))));

    assertThat(csv).contains("컴퓨터공학과").doesNotContain("CSE");
  }

  // 학과가 신설되면 매핑에 없는 코드가 들어온다. 빈칸이 되면 누구 지원선지 알 수 없다.
  @Test
  @DisplayName("모르는 학과 값은 그대로 적는다")
  void unknownMajorStaysAsIs() {
    ApplicantResponse applicant =
        new ApplicantResponse(
            1L,
            7L,
            "홍길동",
            "12201234",
            "NEWDEPT",
            "hong@test.io",
            "01000000000",
            ApplicationStatus.APPLIED,
            EventAttendanceStatus.PENDING,
            Instant.parse("2026-09-01T03:00:00Z"),
            null,
            null,
            Map.of());

    assertThat(text(writer.write(List.of(), List.of(applicant)))).contains("NEWDEPT");
  }

  @Test
  @DisplayName("선택지에서 사라진 값은 저장된 그대로 적는다")
  void unknownOptionValueStaysAsIs() {
    // 운영진이 선택지를 지운 뒤에도 과거 답변은 남는다. 빈 칸으로 만들면 데이터를 잃는다.
    EventFormQuestion q =
        choiceQuestion(
            1L, "식사 여부", QuestionType.SINGLE_CHOICE, List.of(new QuestionOption("OPT_1", "네")));

    String csv = text(writer.write(List.of(q), List.of(applicant(answers(1L, "OPT_GONE")))));

    assertThat(csv).contains("OPT_GONE");
  }

  private static EventFormQuestion choiceQuestion(
      Long id, String label, QuestionType type, List<QuestionOption> options) {
    EventFormQuestion question =
        EventFormQuestion.create(null, type, label, null, false, 0, options, null, null);
    ReflectionTestUtils.setField(question, "id", id);
    return question;
  }

  private static Map<Long, Object> answers(Long questionId, Object value) {
    Map<Long, Object> map = new LinkedHashMap<>();
    map.put(questionId, value);
    return map;
  }

  private static ApplicantResponse applicant(Map<Long, Object> answers) {
    return new ApplicantResponse(
        1L,
        7L,
        "홍길동",
        "12201234",
        // DB 에 실제로 담기는 값은 코드다. 라벨을 넣어 두면 코드 노출 결함을 못 잡는다.
        "CSE",
        "hong@test.io",
        "01000000000",
        ApplicationStatus.APPLIED,
        EventAttendanceStatus.PENDING,
        Instant.parse("2026-09-01T03:00:00Z"),
        null,
        null,
        answers);
  }

  private static EventFormQuestion question(Long id, String label, QuestionType type) {
    List<QuestionOption> options =
        type.isRequiresOptions() ? List.of(new QuestionOption("A", "A")) : null;
    EventFormQuestion question =
        EventFormQuestion.create(null, type, label, null, false, 0, options, null, null);
    ReflectionTestUtils.setField(question, "id", id);
    return question;
  }

  private static String text(byte[] csv) {
    return new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
  }

  private static String firstLine(String csv) {
    return csv.split("\r\n", 2)[0];
  }
}
