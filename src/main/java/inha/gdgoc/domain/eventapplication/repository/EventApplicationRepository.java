package inha.gdgoc.domain.eventapplication.repository;

import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventApplicationRepository extends JpaRepository<EventApplication, Long> {

  /** 폼 수정 허용 범위와 정원을 가르는 기준. 취소한 신청은 세지 않는다. */
  long countByFormIdAndStatus(Long formId, ApplicationStatus status);

  Optional<EventApplication> findByFormIdAndUserId(Long formId, Long userId);

  /**
   * 신청자 목록. status 가 null 이면 취소한 사람까지 보여준다.
   *
   * <p>사용자를 fetch join 하지 않으면 목록 한 쪽마다 사람 수만큼 쿼리가 더 나간다.
   */
  @Query(
      value =
          "select a from EventApplication a join fetch a.user "
              + "where a.form.id = :formId and (:status is null or a.status = :status)",
      countQuery =
          "select count(a) from EventApplication a "
              + "where a.form.id = :formId and (:status is null or a.status = :status)")
  Page<EventApplication> findApplicants(
      @Param("formId") Long formId,
      @Param("status") ApplicationStatus status,
      Pageable pageable);

  /**
   * 마이페이지 활동 이력.
   *
   * <p>폼을 fetch join 해서 행사명·기간을 그 복사본에서 읽는다. 게시판 표를 조인하지 않으므로 글이 휴지통에 들어가도 이력이 비지 않는다.
   */
  @Query(
      "select a from EventApplication a join fetch a.form f "
          + "where a.user.id = :userId and a.status = :status "
          + "order by f.eventStartDate desc, a.appliedAt desc")
  List<EventApplication> findMyActivities(
      @Param("userId") Long userId, @Param("status") ApplicationStatus status);

  /** CSV 로 내보낼 때는 페이지 없이 전부 가져온다. */
  @Query(
      "select a from EventApplication a join fetch a.user "
          + "where a.form.id = :formId and (:status is null or a.status = :status) "
          + "order by a.appliedAt asc")
  List<EventApplication> findAllApplicants(
      @Param("formId") Long formId, @Param("status") ApplicationStatus status);
}
