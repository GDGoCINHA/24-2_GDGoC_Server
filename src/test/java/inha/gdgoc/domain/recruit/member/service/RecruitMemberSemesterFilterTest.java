package inha.gdgoc.domain.recruit.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.recruit.member.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.member.enums.Gender;
import inha.gdgoc.domain.recruit.member.repository.RecruitMemberRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부원 지원자 목록의 학기·이름 필터를 실제 조회로 검증한다.
 *
 * <p>전에는 전체 조회와 이름 검색뿐이라 역대 지원자가 한 목록에 섞여 나왔다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RecruitMemberSemesterFilterTest {

    private static final Pageable PAGE = PageRequest.of(0, 20);

    @Autowired
    private RecruitMemberService recruitMemberService;

    @Autowired
    private RecruitMemberRepository recruitMemberRepository;

    @BeforeEach
    void setUp() {
        recruitMemberRepository.deleteAll();
        recruitMemberRepository.save(member("김가나", "12200001", "01000000001", AdmissionSemester.Y26_2));
        recruitMemberRepository.save(member("이다라", "12200002", "01000000002", AdmissionSemester.Y26_2));
        recruitMemberRepository.save(member("김마바", "12200003", "01000000003", AdmissionSemester.Y26_1));
    }

    @Test
    void 학기로_거르면_그_학기_지원자만_나온다() {
        Page<RecruitMember> result =
            recruitMemberService.searchMembers(null, AdmissionSemester.Y26_2, PAGE);

        assertThat(result.getContent())
            .extracting(RecruitMember::getName)
            .containsExactlyInAnyOrder("김가나", "이다라");
    }

    @Test
    void 이름과_학기를_함께_주면_둘_다_만족하는_지원자만_나온다() {
        Page<RecruitMember> result =
            recruitMemberService.searchMembers("김", AdmissionSemester.Y26_2, PAGE);

        assertThat(result.getContent())
            .extracting(RecruitMember::getName)
            .containsExactly("김가나");
    }

    // 필터를 안 주면 예전과 똑같이 전체가 나와야 한다. 기존 화면이 깨지지 않는지 확인한다.
    @Test
    void 필터가_없으면_전체가_나온다() {
        Page<RecruitMember> result = recruitMemberService.searchMembers(null, null, PAGE);

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void 빈_검색어는_이름_조건으로_치지_않는다() {
        Page<RecruitMember> result = recruitMemberService.searchMembers("   ", null, PAGE);

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    private RecruitMember member(String name, String studentId, String phone, AdmissionSemester semester) {
        return RecruitMember.builder()
            .name(name)
            .studentId(studentId)
            .enrolledClassification(EnrolledClassification.FULL_REGISTRATION)
            .phoneNumber(phone)
            .email(studentId + "@inha.edu")
            .gender(Gender.PRIVATE)
            .birth(LocalDate.of(2003, 3, 1))
            .major("CSE")
            .isPayed(false)
            .admissionSemester(semester)
            .build();
    }
}
