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
   * 이 학번으로 들어와 있는 신청. 계정 신청은 계정의 학번, 로그인 없이 낸 신청은 적어 낸 학번을 본다.
   *
   * <p>한 사람이 계정으로 한 번, 로그인 없이 한 번 낼 수 있어 여러 건이 나올 수 있다.
   */
  @Query(
      "select a from EventApplication a left join a.user u "
          + "where a.form.id = :formId and a.status = :status "
          + "and (u.studentId = :studentId or (u is null and a.applicantStudentId = :studentId))")
  List<EventApplication> findByFormIdAndStudentId(
      @Param("formId") Long formId,
      @Param("studentId") String studentId,
      @Param("status") ApplicationStatus status);

  /**
   * 신청자 목록. status 가 null 이면 취소한 사람까지 보여준다.
   *
   * <p>사용자를 fetch join 하지 않으면 목록 한 쪽마다 사람 수만큼 쿼리가 더 나간다. left join 이어야 한다 — 로그인 없이 낸 신청은
   * 사용자가 없어 inner join 이면 목록에서 사라진다.
   */
  @Query(
      value =
          "select a from EventApplication a left join fetch a.user "
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

  /** CSV 로 내보낼 때는 페이지 없이 전부 가져온다. 목록과 같은 이유로 left join 이다. */
  @Query(
      "select a from EventApplication a left join fetch a.user "
          + "where a.form.id = :formId and (:status is null or a.status = :status) "
          + "order by a.appliedAt asc")
  List<EventApplication> findAllApplicants(
      @Param("formId") Long formId, @Param("status") ApplicationStatus status);
}
