package inha.gdgoc.domain.homecoming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.homecoming.dto.request.HomecomingApplyRequest;
import inha.gdgoc.domain.homecoming.dto.request.HomecomingUpdateRequest;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingApplicationResponse;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingApplyResponse;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingSummaryResponse;
import inha.gdgoc.domain.homecoming.entity.HomecomingApplication;
import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.ConsentPurpose;
import inha.gdgoc.domain.homecoming.enums.ConsentState;
import inha.gdgoc.domain.homecoming.enums.HomecomingMemberRole;
import inha.gdgoc.domain.homecoming.enums.HomecomingStatus;
import inha.gdgoc.domain.homecoming.enums.HomecomingTeam;
import inha.gdgoc.domain.homecoming.exception.HomecomingErrorCode;
import inha.gdgoc.domain.homecoming.repository.HomecomingApplicationRepository;
import inha.gdgoc.domain.homecoming.repository.HomecomingConsentRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 신청 저장·중복·마감·관리 흐름을 실제 DB(H2)와 함께 본다. 저장이 별도 트랜잭션(Writer)에서 일어나므로 테스트 클래스에 트랜잭션을 걸지 않고
 * 매번 지운다.
 */
@ActiveProfiles("test")
@SpringBootTest
class HomecomingApplicationServiceTest {

  private static final Instant CLOSE_AT = Instant.parse("2026-11-22T14:59:59Z"); // 11/22 23:59:59 KST
  private static final Instant BEFORE = Instant.parse("2026-11-20T03:00:00Z");

  @Autowired private HomecomingApplicationRepository applicationRepository;
  @Autowired private HomecomingConsentRepository consentRepository;
  @Autowired private HomecomingApplicationWriter writer;
  @Autowired private HomecomingProfileNormalizer normalizer;
  @Autowired private HomecomingCsvWriter csvWriter;

  /** 쓰기(취소·수정·철회)는 트랜잭션 프록시가 있는 빈으로 해야 반영된다. 시계를 고정한 인스턴스는 읽기 전용으로만 쓴다. */
  @Autowired private HomecomingAdminService admin;

  private HomecomingApplicationService service;

  @BeforeEach
  void setUp() {
    service = at(BEFORE);
  }

  @AfterEach
  void tearDown() {
    consentRepository.deleteAll();
    applicationRepository.deleteAll();
  }

  private HomecomingApplicationService at(Instant now) {
    return new HomecomingApplicationService(
        applicationRepository, writer, normalizer, CLOSE_AT, Clock.fixed(now, ZoneOffset.UTC));
  }

  private static HomecomingApplyRequest req(String requestId, AttendType type, String phone) {
    return new HomecomingApplyRequest(
        requestId, type, " 홍길동 ", phone, 2022, 2024, HomecomingMemberRole.ORGANIZER, HomecomingTeam.BD,
        true, "15:00", "채식 메뉴 부탁드려요", true, false, true, "2026-11-12.draft");
  }

  private static String rid() {
    return UUID.randomUUID().toString();
  }

  @Test
  @DisplayName("본 행사 신청은 모든 칸을 저장하고 목적별 동의 3건을 남긴다")
  void savesMainApplication() {
    HomecomingApplyResponse res = service.submit(req(rid(), AttendType.MAIN, "01012345678"));

    HomecomingApplication saved = applicationRepository.findById(res.id()).orElseThrow();
    assertThat(saved.getName()).isEqualTo("홍길동");
    assertThat(saved.getPhone()).isEqualTo("010-1234-5678");
    assertThat(saved.getTeam()).isEqualTo(HomecomingTeam.BD);
    assertThat(saved.getArrivalTime()).isEqualTo("15:00");
    assertThat(saved.getMessage()).isEqualTo("채식 메뉴 부탁드려요");
    assertThat(consentRepository.findAllByApplicationId(res.id())).hasSize(3);
    assertThat(consentRepository.findByApplicationIdAndPurpose(res.id(), ConsentPurpose.TALENT_POOL).orElseThrow().getExpiresAt())
        .isEqualTo(HomecomingApplicationService.OPTIONAL_CONSENT_EXPIRES_AT);
    assertThat(consentRepository.findByApplicationIdAndPurpose(res.id(), ConsentPurpose.EVENT_INVITE).orElseThrow().getExpiresAt())
        .as("거부한 동의에는 기한을 두지 않는다")
        .isNull();
  }

  @Test
  @DisplayName("불참은 활동·도착·요청사항을 버리고, 뒤풀이만은 도착·요청사항을 버린다")
  void dropsFieldsTheTypeDoesNotCollect() {
    HomecomingApplication no =
        applicationRepository.findById(service.submit(req(rid(), AttendType.NO, "010-1111-1111")).id()).orElseThrow();
    assertThat(no.getYearStart()).isNull();
    assertThat(no.getMemberRole()).isNull();
    assertThat(no.getTeam()).isNull();
    assertThat(no.isLate()).isFalse();
    assertThat(no.getArrivalTime()).isNull();
    assertThat(no.getMessage()).isNull();

    HomecomingApplication after =
        applicationRepository.findById(service.submit(req(rid(), AttendType.AFTER, "010-2222-2222")).id()).orElseThrow();
    assertThat(after.getTeam()).isEqualTo(HomecomingTeam.BD);
    assertThat(after.isLate()).isFalse();
    assertThat(after.getArrivalTime()).isNull();
    assertThat(after.getMessage()).isNull();
  }

  @Test
  @DisplayName("같은 requestId 재시도는 새 행을 만들지 않고 첫 결과를 돌려준다 — 마감 후에도")
  void replayIsIdempotentEvenAfterClose() {
    String id = rid();
    HomecomingApplyResponse first = service.submit(req(id, AttendType.MAIN, "010-3333-3333"));
    HomecomingApplyResponse again = at(CLOSE_AT.plusSeconds(60)).submit(req(id, AttendType.MAIN, "010-3333-3333"));

    assertThat(again.id()).isEqualTo(first.id());
    assertThat(applicationRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("같은 번호(하이픈 유무 무관)는 409, 운영진이 취소하면 다시 신청할 수 있다")
  void duplicatePhone() {
    HomecomingApplyResponse first = service.submit(req(rid(), AttendType.MAIN, "010-4444-4444"));

    assertThatThrownBy(() -> service.submit(req(rid(), AttendType.NO, "01044444444")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.DUPLICATE_PHONE);

    admin.cancel(first.id(), "변경 요청", 7L);
    assertThat(service.submit(req(rid(), AttendType.NO, "01044444444")).attendType()).isEqualTo(AttendType.NO);
  }

  @Test
  @DisplayName("마감 시각이 지나면 새 신청은 403(CLOSED)")
  void closed() {
    assertThatThrownBy(() -> at(CLOSE_AT.plusSeconds(1)).submit(req(rid(), AttendType.MAIN, "010-5555-5555")))
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.CLOSED);
    assertThat(at(CLOSE_AT).submit(req(rid(), AttendType.MAIN, "010-5555-5555")).id()).isNotNull();
  }

  @Test
  @DisplayName("마감 시각이 없으면 언제든 받는다")
  void noDeadline() {
    HomecomingApplicationService open =
        new HomecomingApplicationService(
            applicationRepository, writer, normalizer, null, Clock.fixed(CLOSE_AT.plusSeconds(86_400L * 365), ZoneOffset.UTC));

    assertThat(open.isClosed()).isFalse();
    assertThat(open.closeAt()).isNull();
    assertThat(open.submit(req(rid(), AttendType.MAIN, "010-5656-5656")).id()).isNotNull();
  }

  @Test
  @DisplayName("필수 동의가 없으면 저장하지 않는다")
  void privacyRequired() {
    HomecomingApplyRequest r = req(rid(), AttendType.MAIN, "010-6666-6666");
    HomecomingApplyRequest noPrivacy =
        new HomecomingApplyRequest(
            r.requestId(), r.attendType(), r.name(), r.phone(), r.yearStart(), r.yearEnd(), r.memberRole(), r.team(),
            r.isLate(), r.arrivalTime(), r.message(), false, true, true, r.consentVersion());

    assertThatThrownBy(() -> service.submit(noPrivacy))
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.PRIVACY_REQUIRED);
    assertThat(applicationRepository.count()).isZero();
  }

  @Test
  @DisplayName("관리자 수정: 일반 멤버로 바꾸면 팀이 지워지고, 다른 신청의 번호로는 바꿀 수 없다")
  void adminUpdate() {
    Long a = service.submit(req(rid(), AttendType.MAIN, "010-7777-7777")).id();
    service.submit(req(rid(), AttendType.MAIN, "010-8888-8888"));

    HomecomingApplicationResponse updated =
        admin.update(
            a,
            new HomecomingUpdateRequest(
                AttendType.BOTH, "홍길동", "010-7777-7777", 2023, 2023, HomecomingMemberRole.MEMBER,
                HomecomingTeam.BD, false, null, null),
            9L);
    assertThat(updated.team()).isNull();
    assertThat(updated.activitySummary()).isEqualTo("2023 ~ 2023 · 일반 멤버");

    assertThatThrownBy(
            () ->
                admin.update(
                    a,
                    new HomecomingUpdateRequest(
                        AttendType.BOTH, "홍길동", "010-8888-8888", 2023, 2023, HomecomingMemberRole.MEMBER,
                        null, false, null, null),
                    9L))
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.DUPLICATE_PHONE);
  }

  @Test
  @DisplayName("집계는 취소를 빼고 유형별·본 행사·뒤풀이 인원을 센다")
  void summary() {
    service.submit(req(rid(), AttendType.MAIN, "010-1000-0001"));
    service.submit(req(rid(), AttendType.BOTH, "010-1000-0002"));
    service.submit(req(rid(), AttendType.AFTER, "010-1000-0003"));
    service.submit(req(rid(), AttendType.MAYBE, "010-1000-0004"));
    Long canceled = service.submit(req(rid(), AttendType.MAIN, "010-1000-0005")).id();
    admin.cancel(canceled, null, 1L);

    HomecomingSummaryResponse s = admin.summary();
    assertThat(s.main()).isEqualTo(1);
    assertThat(s.both()).isEqualTo(1);
    assertThat(s.after()).isEqualTo(1);
    assertThat(s.maybe()).isEqualTo(1);
    assertThat(s.total()).isEqualTo(4);
    assertThat(s.mainEventAttendees()).isEqualTo(2);
    assertThat(s.afterPartyAttendees()).isEqualTo(2);
    assertThat(s.lateArrivals()).isEqualTo(2);
    assertThat(s.canceled()).isEqualTo(1);
  }

  @Test
  @DisplayName("명찰 CSV 는 본 행사 참석자만, 취소 건 없이 담는다")
  void nametags() {
    service.submit(req(rid(), AttendType.MAIN, "010-2000-0001"));
    service.submit(req(rid(), AttendType.AFTER, "010-2000-0002"));
    Long canceled = service.submit(req(rid(), AttendType.BOTH, "010-2000-0003")).id();
    admin.cancel(canceled, null, 1L);

    String csv = new String(admin.exportNametags(), StandardCharsets.UTF_8);
    assertThat(csv.lines().count()).isEqualTo(2); // 헤더 + main 1건
    assertThat(csv).contains("2022 ~ 2024 · 운영진 · BD");
  }

  @Test
  @DisplayName("장기 등록: 동의한 사람만 나오고, 철회하면 WITHDRAWN, 기한이 지나면 EXPIRED")
  void longTermConsents() {
    Long id = service.submit(req(rid(), AttendType.MAIN, "010-3000-0001")).id(); // 인재풀만 동의

    assertThat(admin.consents(ConsentPurpose.EVENT_INVITE, null)).isEmpty();
    assertThat(admin.consents(ConsentPurpose.TALENT_POOL, ConsentState.ACTIVE)).hasSize(1);

    HomecomingAdminService later =
        new HomecomingAdminService(
            applicationRepository, consentRepository, normalizer, csvWriter,
            Clock.fixed(HomecomingApplicationService.OPTIONAL_CONSENT_EXPIRES_AT, ZoneOffset.UTC));
    assertThat(later.consents(ConsentPurpose.TALENT_POOL, ConsentState.EXPIRED)).hasSize(1);

    admin.withdrawConsent(id, ConsentPurpose.TALENT_POOL, 3L);
    assertThat(admin.consents(ConsentPurpose.TALENT_POOL, ConsentState.WITHDRAWN)).hasSize(1);
    assertThat(admin.get(id).talentPool()).isEqualTo(ConsentState.WITHDRAWN);

    assertThatThrownBy(() -> admin.withdrawConsent(id, ConsentPurpose.EVENT_INVITE, 3L))
        .extracting("errorCode")
        .isEqualTo(HomecomingErrorCode.CONSENT_NOT_AGREED);
    assertThat(admin.list(HomecomingStatus.ACTIVE, null)).hasSize(1);
  }
}
