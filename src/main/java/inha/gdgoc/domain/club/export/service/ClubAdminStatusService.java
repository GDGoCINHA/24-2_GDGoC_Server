package inha.gdgoc.domain.club.export.service;

import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.repository.ClubTermRepository;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning;
import inha.gdgoc.domain.club.completion.exception.ClubCompletionErrorCode;
import inha.gdgoc.domain.club.completion.service.ClubCompletionService;
import inha.gdgoc.domain.club.export.dto.AdminClubRowResponse;
import inha.gdgoc.domain.club.export.repository.ClubAdminQueryRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영진 현황 표. 팀마다 완주 계산을 그대로 돌린다 — 한 기수의 팀은 수십 개라 팀당 쿼리 몇 번은 감당할 만하고, 팀 화면과 숫자가 어긋나지 않는 게
 * 더 중요하다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubAdminStatusService {

  /** 경고가 하나라도 있는 팀만 거르는 값. 그 밖에는 {@link ClubCompletionWarning} 이름 하나. */
  public static final String ANY_WARNING = "ANY";

  private final ClubAdminQueryRepository clubAdminQueryRepository;
  private final ClubTermRepository clubTermRepository;
  private final ClubCompletionService clubCompletionService;

  /**
   * @param termId null 이면 가장 최근 기수. 기수가 하나도 없으면 빈 목록
   * @param warning 비어 있으면 전체, {@code ANY} 면 경고 있는 팀, 경고 이름이면 그 경고가 있는 팀
   */
  public List<AdminClubRowResponse> findRows(Long termId, String warning) {
    Predicate<AdminClubRowResponse> filter = filterOf(warning);
    Optional<Long> resolved =
        termId != null
            ? Optional.of(termId)
            : clubTermRepository.findTopByOrderByIdDesc().map(ClubTerm::getId);
    if (resolved.isEmpty()) {
      return List.of();
    }
    return clubAdminQueryRepository.findAllByTerm(resolved.get()).stream()
        .map(club -> AdminClubRowResponse.of(club, clubCompletionService.getCompletion(club)))
        .filter(filter)
        .toList();
  }

  private static Predicate<AdminClubRowResponse> filterOf(String warning) {
    if (warning == null || warning.isBlank()) {
      return r -> true;
    }
    if (ANY_WARNING.equals(warning)) {
      return r -> !r.warnings().isEmpty();
    }
    try {
      ClubCompletionWarning w = ClubCompletionWarning.valueOf(warning);
      return r -> r.warnings().contains(w);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ClubCompletionErrorCode.WARNING_FILTER_INVALID);
    }
  }
}
