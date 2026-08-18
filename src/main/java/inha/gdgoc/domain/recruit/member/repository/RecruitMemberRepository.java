package inha.gdgoc.domain.recruit.member.repository;

import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecruitMemberRepository
        extends JpaRepository<RecruitMember, Long>, JpaSpecificationExecutor<RecruitMember> {

    /**
     * 중복 판정은 전부 <b>학기 단위</b>다. 코어의 {@code findByUserIdAndSession} 과 같은 역할이다.
     *
     * <p>전에는 세 검사 모두 전역이라 지난 학기 지원자가 이번 학기에 다시 낼 수 없었다.
     * 화면은 「중복된 학번입니다」로 막았고, 그걸 넘겨도 DB 의 전역 UNIQUE 가 500 을 냈다.
     */
    boolean existsByStudentIdAndAdmissionSemester(String studentId, AdmissionSemester admissionSemester);

    boolean existsByPhoneNumberAndAdmissionSemester(String phoneNumber, AdmissionSemester admissionSemester);

    boolean existsByEmailIgnoreCaseAndAdmissionSemester(String email, AdmissionSemester admissionSemester);

    /** 부원 지원 알림(메모) 신청에서 쓴다. 그쪽은 학기 개념 없이 「이미 지원한 사람인가」만 본다. */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 마이페이지에서 본인의 <b>이번 학기</b> 지원서를 찾을 때 쓴다.
     *
     * <p>부원 지원은 비로그인으로 받으므로 계정과 지원서를 잇는 키가 이메일뿐이다.
     * 로그인(@inha.edu 전용)과 지원 폼(도메인 @inha.edu 고정)이 같은 도메인이라 성립한다.
     *
     * <p>학기를 거는 이유: 지난 학기 지원서를 「신청 현황」에 띄우면 이번 학기에 낸 것으로 읽힌다.
     * 실제로 2026-1 에 낸 지원서가 2026-2 화면에 그대로 보였다.
     */
    Optional<RecruitMember> findByEmailIgnoreCaseAndAdmissionSemester(
            String email, AdmissionSemester admissionSemester);
}
