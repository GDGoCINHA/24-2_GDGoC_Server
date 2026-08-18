package inha.gdgoc.domain.recruit.member.repository;

import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecruitMemberRepository
        extends JpaRepository<RecruitMember, Long>, JpaSpecificationExecutor<RecruitMember> {
    boolean existsByStudentId(String studentId);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmailIgnoreCase(String email);

    /** 학기당 이메일 1회 제한. 코어의 {@code findByUserIdAndSession} 과 같은 역할이다. */
    boolean existsByEmailIgnoreCaseAndAdmissionSemester(String email, AdmissionSemester admissionSemester);

    /**
     * 마이페이지에서 본인 지원서를 찾을 때 쓴다.
     *
     * <p>부원 지원은 비로그인으로 받으므로 계정과 지원서를 잇는 키가 이메일뿐이다.
     * 로그인(@inha.edu 전용)과 지원 폼(도메인 @inha.edu 고정)이 같은 도메인이라 성립한다.
     * 학기를 안 거는 이유는 지난 학기 지원서도 본인 것이면 보여야 하기 때문이다.
     */
    Optional<RecruitMember> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
