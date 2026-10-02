package inha.gdgoc.domain.club.activity.service;

import inha.gdgoc.domain.club.activity.dto.ApprovedActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.domain.club.activity.repository.ClubActivityRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 활동 기록 조회. C(인증 검토·완주 계산)가 부른다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubActivityQueryService {

  private final ClubActivityRepository clubActivityRepository;

  /**
   * 인증 완료된 활동을 활동일 순으로. {@code from}·{@code to} 는 둘 다 포함한다.
   *
   * <p>활동 기간 밖의 기록을 빼고 경고로 보여주는 것은 부르는 쪽(C)의 일이다. 여기는 받은 기간을 그대로 쓴다.
   */
  public List<ApprovedActivity> findApproved(Long clubId, LocalDate from, LocalDate to) {
    return clubActivityRepository.summarizeByStatus(
        clubId, ClubActivityStatus.APPROVED, from, to);
  }

  /**
   * 상태를 바꿀 기록을 잠가서 읽는다. 검토({@link ClubActivity#approve}·{@link ClubActivity#requestRevision})와 출석 수정
   * 수락은 반드시 이걸로 읽는다.
   *
   * <p>잠금은 부르는 쪽 트랜잭션이 끝날 때 풀리므로 트랜잭션 안에서만 부를 수 있다({@code MANDATORY}). 읽기 전용이 아닌 것도 같은 이유다 —
   * PostgreSQL 은 읽기 전용 트랜잭션의 {@code SELECT ... FOR UPDATE} 를 거부한다.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public ClubActivity getForUpdate(Long activityId) {
    return clubActivityRepository
        .findByIdForUpdate(activityId)
        .orElseThrow(() -> new BusinessException(ClubActivityErrorCode.ACTIVITY_NOT_FOUND));
  }
}
