package inha.gdgoc.domain.recruit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.recruit.member.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.recruit.member.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.member.enums.Gender;
import inha.gdgoc.domain.recruit.member.exception.RecruitMemberErrorCode;
import inha.gdgoc.domain.recruit.member.exception.RecruitMemberException;
import inha.gdgoc.domain.recruit.member.repository.RecruitMemberRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부원 지원의 "이메일당 학기 1회" 제한과 본인 지원서 조회를 검증한다.
 *
 * <p>전에는 서버가 중복을 전혀 보지 않았다. 화면의 중복 확인 버튼이 유일한 장치였고, 지원 API 는
 * 인증 없이 열려 있어 주소를 직접 치면 같은 사람이 몇 번이든 지원할 수 있었다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RecruitMemberApplicationLimitTest {

    @Autowired
    private RecruitMemberService recruitMemberService;

    @Autowired
    private RecruitMemberRepository recruitMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        recruitMemberRepository.deleteAll();
    }

    @Test
    void 같은_이메일로_같은_학기에_두_번_지원하면_막힌다() {
        recruitMemberService.addRecruitMember(payload("12200001", "01000000001", "hong@inha.edu"), null);

        // 학번·전화번호를 바꿔도 이메일이 같으면 막혀야 한다.
        assertThatThrownBy(() ->
            recruitMemberService.addRecruitMember(payload("12200002", "01000000002", "hong@inha.edu"), null))
            .isInstanceOf(RecruitMemberException.class)
            .hasFieldOrPropertyWithValue("errorCode", RecruitMemberErrorCode.RECRUIT_MEMBER_ALREADY_APPLIED);

        assertThat(recruitMemberRepository.count()).isEqualTo(1);
    }

    // 이메일은 대소문자를 가리지 않는다. Hong@ 으로 다시 내는 우회를 막는다.
    @Test
    void 대소문자만_다른_이메일도_같은_지원으로_본다() {
        recruitMemberService.addRecruitMember(payload("12200001", "01000000001", "hong@inha.edu"), null);

        assertThatThrownBy(() ->
            recruitMemberService.addRecruitMember(payload("12200002", "01000000002", "HONG@inha.edu"), null))
            .isInstanceOf(RecruitMemberException.class);
    }

    @Test
    void 이메일이_다르면_지원할_수_있다() {
        recruitMemberService.addRecruitMember(payload("12200001", "01000000001", "hong@inha.edu"), null);
        recruitMemberService.addRecruitMember(payload("12200002", "01000000002", "kim@inha.edu"), null);

        assertThat(recruitMemberRepository.count()).isEqualTo(2);
    }

    @Test
    void 마이페이지에서_계정_이메일과_같은_지원서를_본다() {
        recruitMemberRepository.save(member("12200001", "01000000001", "hong@inha.edu"));
        User user = userRepository.save(user("hong@inha.edu"));

        SpecifiedMemberResponse response = recruitMemberService.findMyApplication(user.getId());

        assertThat(response.email()).isEqualTo("hong@inha.edu");
        assertThat(response.studentId()).isEqualTo("12200001");
        assertThat(response.admissionSemester()).isEqualTo(AdmissionSemester.Y26_2);
    }

    // 남의 지원서가 새어 나가면 안 된다. 이메일이 다르면 없는 것으로 본다.
    @Test
    void 지원_이력이_없으면_찾을_수_없다() {
        recruitMemberRepository.save(member("12200001", "01000000001", "hong@inha.edu"));
        User user = userRepository.save(user("kim@inha.edu"));

        assertThatThrownBy(() -> recruitMemberService.findMyApplication(user.getId()))
            .isInstanceOf(RecruitMemberException.class)
            .hasFieldOrPropertyWithValue("errorCode", RecruitMemberErrorCode.RECRUIT_MEMBER_NOT_FOUND);
    }

    private Map<String, Object> payload(String studentId, String phoneNumber, String email) {
        Map<String, Object> member = new HashMap<>();
        member.put("name", "홍길동");
        member.put("studentId", studentId);
        member.put("enrolledClassification", "재학");
        member.put("phoneNumber", phoneNumber);
        member.put("email", email);
        member.put("gender", "남성");
        member.put("birth", "2003.03.01");
        member.put("major", "컴퓨터공학과");
        member.put("isPayed", false);

        Map<String, Object> answers = new HashMap<>();
        answers.put("gdgInterest", List.of("BackEnd"));
        answers.put("gdgWish", List.of("스터디"));
        answers.put("gdgFeedback", "없습니다");

        Map<String, Object> payload = new HashMap<>();
        payload.put("member", member);
        payload.put("answers", answers);
        return payload;
    }

    private RecruitMember member(String studentId, String phoneNumber, String email) {
        return RecruitMember.builder()
            .name("홍길동")
            .studentId(studentId)
            .enrolledClassification(EnrolledClassification.FULL_REGISTRATION)
            .phoneNumber(phoneNumber)
            .email(email)
            .gender(Gender.PRIVATE)
            .birth(LocalDate.of(2003, 3, 1))
            .major("CSE")
            .isPayed(false)
            .admissionSemester(AdmissionSemester.Y26_2)
            .build();
    }

    private User user(String email) {
        return User.builder()
            .name("홍길동")
            .oauthSubject("oauth-" + email)
            .major("CSE")
            .studentId("12200001")
            .phoneNumber("01000000001")
            .email(email)
            .build();
    }
}
