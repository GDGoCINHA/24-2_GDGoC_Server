package inha.gdgoc.domain.club.export.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.repository.ClubTermRepository;
import inha.gdgoc.domain.club.completion.dto.response.ClubCompletionResponse;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionStatus;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import inha.gdgoc.domain.club.completion.exception.ClubCompletionErrorCode;
import inha.gdgoc.domain.club.completion.service.ClubCompletionService;
import inha.gdgoc.domain.club.export.dto.AdminClubRowResponse;
import inha.gdgoc.domain.club.export.repository.ClubAdminQueryRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClubAdminStatusServiceTest {

  private static final Long TERM_ID = 3L;

  @Mock private ClubAdminQueryRepository clubAdminQueryRepository;
  @Mock private ClubTermRepository clubTermRepository;
  @Mock private ClubCompletionService clubCompletionService;

  private ClubAdminStatusService service;
  private Club clean;
  private Club warned;

  @BeforeEach
  void setUp() {
    service =
        new ClubAdminStatusService(
            clubAdminQueryRepository, clubTermRepository, clubCompletionService);
    clean = club(1L);
    warned = club(2L);
    given(clubAdminQueryRepository.findAllByTerm(TERM_ID)).willReturn(List.of(clean, warned));
    given(clubCompletionService.getCompletion(clean)).willReturn(completion(List.of()));
    given(clubCompletionService.getCompletion(warned))
        .willReturn(completion(List.of(ClubCompletionWarning.MEMBERS_UNDER_4)));
  }

  private static Club club(Long id) {
    Club c = BeanUtils.instantiateClass(Club.class);
    ReflectionTestUtils.setField(c, "id", id);
    ReflectionTestUtils.setField(c, "leader", User.builder().name("리더").build());
    return c;
  }

  private static ClubCompletionResponse completion(List<ClubCompletionWarning> warnings) {
    return new ClubCompletionResponse(
        null, List.of(), List.of(), 4, true, ClubGoalStatus.NOT_SUBMITTED, 0, 0, false, warnings,
        null, null, null, List.of(), ClubCompletionStatus.IN_PROGRESS, null, null);
  }

  @Test
  @DisplayName("필터가 없으면 전체, ANY 면 경고 있는 팀, 이름이면 그 경고가 있는 팀")
  void filters() {
    assertThat(service.findRows(TERM_ID, null)).extracting(AdminClubRowResponse::clubId)
        .containsExactly(1L, 2L);
    assertThat(service.findRows(TERM_ID, "ANY")).extracting(AdminClubRowResponse::clubId)
        .containsExactly(2L);
    assertThat(service.findRows(TERM_ID, "MEMBERS_UNDER_4"))
        .extracting(AdminClubRowResponse::clubId)
        .containsExactly(2L);
    assertThat(service.findRows(TERM_ID, "GOAL_NOT_SET")).isEmpty();
  }

  @Test
  @DisplayName("모르는 경고 이름은 400")
  void unknownWarningIsBadRequest() {
    assertThatThrownBy(() -> service.findRows(TERM_ID, "NOPE"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ClubCompletionErrorCode.WARNING_FILTER_INVALID);
  }

  @Test
  @DisplayName("기수를 안 주면 가장 최근 기수, 기수가 없으면 빈 목록")
  void defaultsToLatestTerm() {
    ClubTerm latest = ClubTerm.create("2026-2", new BigDecimal("0.50"));
    ReflectionTestUtils.setField(latest, "id", TERM_ID);
    given(clubTermRepository.findTopByOrderByIdDesc()).willReturn(Optional.of(latest));
    assertThat(service.findRows(null, null)).hasSize(2);

    given(clubTermRepository.findTopByOrderByIdDesc()).willReturn(Optional.empty());
    assertThat(service.findRows(null, null)).isEmpty();
  }
}
