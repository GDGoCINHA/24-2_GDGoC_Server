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
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.util.SemesterCalculator;
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
 * 부원 지원의 "학기당 1회" 제한과 본인 지원서 조회를 검증한다.
 *
 * <p>전에는 서버가 중복을 전혀 보지 않았다. 화면의 중복 확인 버튼이 유일한 장치였고, 지원 API 는
 * 인증 없이 열려 있어 주소를 직접 치면 같은 사람이 몇 번이든 지원할 수 있었다.
 *
 * <p>반대로 <b>학기가 바뀌어도 재지원이 안 되는</b> 문제도 있었다. 학번·전화번호에 전역 UNIQUE 가
 * 걸려 있어 2026-1 지원자가 2026-2 에 다시 내면 제약 위반 500 이 났다. 이제 (값, 학기) 복합이다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RecruitMemberApplicationLimitTest {

    /** 실행 시점이 언제든 반드시 과거인 학기. enum 의 첫 상수라 현재 학기가 될 일이 없다. */
    private static final AdmissionSemester PAST_SEMESTER = AdmissionSemester.Y21_2;

    @Autowired
    private RecruitMemberService recruitMemberService;

    @Autowired
    private RecruitMemberRepository recruitMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SemesterCalculator semesterCalculator;

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

    // 학기가 바뀌면 같은 사람이 다시 낼 수 있어야 한다. 매 학기 회비를 받는 운영이 그렇다.
    @Test
    void 지난_학기에_지원했어도_이번_학기에_다시_지원할_수_있다() {
        recruitMemberRepository.save(
            member("12200001", "01000000001", "hong@inha.edu", PAST_SEMESTER));

        // 같은 사람(학번·전화·이메일 전부 동일)이 이번 학기에 다시 낸다.
        recruitMemberService.addRecruitMember(
            payload("12200001", "01000000001", "hong@inha.edu"), null);

        assertThat(recruitMemberRepository.count()).isEqualTo(2);
    }

    @Test
    void 같은_학기에_학번이_겹치면_막힌다() {
        recruitMemberService.addRecruitMember(payload("12200001", "01000000001", "a@inha.edu"), null);

        assertThatThrownBy(() ->
            recruitMemberService.addRecruitMember(payload("12200001", "01000000002", "b@inha.edu"), null))
            .isInstanceOf(RecruitMemberException.class)
            .hasFieldOrPropertyWithValue("errorCode", RecruitMemberErrorCode.RECRUIT_MEMBER_ALREADY_APPLIED);
    }

    @Test
    void 같은_학기에_전화번호가_겹치면_막힌다() {
        recruitMemberService.addRecruitMember(payload("12200001", "01000000001", "a@inha.edu"), null);

        assertThatThrownBy(() ->
            recruitMemberService.addRecruitMember(payload("12200002", "01000000001", "b@inha.edu"), null))
            .isInstanceOf(RecruitMemberException.class)
            .hasFieldOrPropertyWithValue("errorCode", RecruitMemberErrorCode.RECRUIT_MEMBER_ALREADY_APPLIED);
    }

    // 폼의 중복 확인도 이번 학기만 봐야 한다. 전역이면 지난 학기 지원자가 시작조차 못 한다.
    @Test
    void 중복_확인_3종은_지난_학기_지원서를_세지_않는다() {
        recruitMemberRepository.save(
            member("12200001", "01000000001", "hong@inha.edu", PAST_SEMESTER));

        assertThat(recruitMemberService.isRegisteredStudentId("12200001").isExists()).isFalse();
        assertThat(recruitMemberService.isRegisteredPhoneNumber("01000000001").isExists()).isFalse();
        assertThat(recruitMemberService.isRegisteredEmail("hong@inha.edu").isExists()).isFalse();
    }

    @Test
    void 중복_확인_3종은_이번_학기_지원서를_센다() {
        recruitMemberService.addRecruitMember(payload("12200001", "01000000001", "hong@inha.edu"), null);

        assertThat(recruitMemberService.isRegisteredStudentId("12200001").isExists()).isTrue();
        assertThat(recruitMemberService.isRegisteredPhoneNumber("01000000001").isExists()).isTrue();
        assertThat(recruitMemberService.isRegisteredEmail("hong@inha.edu").isExists()).isTrue();
    }

    // 지난 학기 지원서를 「신청 현황」에 띄우면 이번 학기에 낸 것으로 읽힌다.
    @Test
    void 마이페이지는_지난_학기_지원서를_보여주지_않는다() {
        recruitMemberRepository.save(
            member("12200001", "01000000001", "hong@inha.edu", PAST_SEMESTER));
        User user = userRepository.save(user("hong@inha.edu"));

        assertThatThrownBy(() -> recruitMemberService.findMyApplication(user.getId()))
            .isInstanceOf(RecruitMemberException.class)
            .hasFieldOrPropertyWithValue("errorCode", RecruitMemberErrorCode.RECRUIT_MEMBER_NOT_FOUND);
    }

    @Test
    void 마이페이지에서_계정_이메일과_같은_지원서를_본다() {
        recruitMemberRepository.save(member("12200001", "01000000001", "hong@inha.edu"));
        User user = userRepository.save(user("hong@inha.edu"));

        SpecifiedMemberResponse response = recruitMemberService.findMyApplication(user.getId());

        assertThat(response.email()).isEqualTo("hong@inha.edu");
        assertThat(response.studentId()).isEqualTo("12200001");
        assertThat(response.admissionSemester()).isEqualTo(currentSemester());
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

    // 이메일이 지원서와 계정을 잇는 유일한 키다. 다른 도메인으로 들어오면 나중에 로그인해도
    // 영영 이어지지 않아 MEMBER 승격이 안 된다. 화면이 도메인을 고정하지만 API 는 열려 있다.
    @Test
    void 인하대_이메일이_아니면_지원할_수_없다() {
        assertThatThrownBy(() ->
            recruitMemberService.addRecruitMember(payload("12200001", "01000000001", "hong@gmail.com"), null))
            .isInstanceOf(RecruitMemberException.class)
            .hasFieldOrPropertyWithValue("errorCode", RecruitMemberErrorCode.RECRUIT_MEMBER_INVALID_EMAIL_DOMAIN);

        assertThat(recruitMemberRepository.count()).isZero();
    }

    @Test
    void 인하대_이메일은_대소문자를_가리지_않는다() {
        recruitMemberService.addRecruitMember(payload("12200001", "01000000001", "hong@INHA.edu"), null);

        assertThat(recruitMemberRepository.count()).isEqualTo(1);
    }

    // inha.edu 로 끝나기만 하면 통과하는 검사는 fake-inha.edu 같은 도메인을 놓친다.
    @Test
    void 인하대_도메인을_흉내낸_주소는_막힌다() {
        assertThatThrownBy(() ->
            recruitMemberService.addRecruitMember(payload("12200001", "01000000001", "hong@fake-inha.edu"), null))
            .isInstanceOf(RecruitMemberException.class)
            .hasFieldOrPropertyWithValue("errorCode", RecruitMemberErrorCode.RECRUIT_MEMBER_INVALID_EMAIL_DOMAIN);
    }

    // ===== 입금 체크와 계정 역할 =====
    // 지원서에는 계정을 가리키는 컬럼이 없다. 이메일로 이어 붙이는 이 연결이 끊기면
    // 회비를 내고도 GUEST 로 남아 자유게시판에 글을 쓰지 못한다.

    @Test
    void 입금_완료로_바꾸면_GUEST_계정이_MEMBER_가_된다() {
        RecruitMember application = recruitMemberRepository.save(member("12200001", "01000000001", "hong@inha.edu"));
        User account = userRepository.save(user("hong@inha.edu"));

        recruitMemberService.updatePayment(application.getId(), true);

        assertThat(roleOf(account)).isEqualTo(UserRole.MEMBER);
    }

    // 잘못 누른 체크를 되돌리면 권한도 함께 돌아와야 한다.
    @Test
    void 미입금으로_되돌리면_MEMBER_계정이_GUEST_가_된다() {
        RecruitMember application = recruitMemberRepository.save(member("12200001", "01000000001", "hong@inha.edu"));
        User account = userRepository.save(user("hong@inha.edu"));
        recruitMemberService.updatePayment(application.getId(), true);

        recruitMemberService.updatePayment(application.getId(), false);

        assertThat(roleOf(account)).isEqualTo(UserRole.GUEST);
    }

    // CORE 이상은 회비와 무관하게 임명된 자리다. 오클릭 한 번으로 운영진이 GUEST 가 되면 안 된다.
    @Test
    void 미입금으로_되돌려도_CORE_는_강등되지_않는다() {
        RecruitMember application = recruitMemberRepository.save(member("12200001", "01000000001", "hong@inha.edu"));
        User account = userRepository.save(user("hong@inha.edu"));
        account.changeRole(UserRole.CORE);
        userRepository.save(account);

        recruitMemberService.updatePayment(application.getId(), true);
        recruitMemberService.updatePayment(application.getId(), false);

        assertThat(roleOf(account)).isEqualTo(UserRole.CORE);
    }

    // 지난 학기 지원서의 입금 체크로 이번 학기 권한이 움직이면 안 된다.
    @Test
    void 지난_학기_지원서의_입금_체크는_역할을_바꾸지_않는다() {
        RecruitMember application = recruitMemberRepository.save(
            member("12200001", "01000000001", "hong@inha.edu", PAST_SEMESTER));
        User account = userRepository.save(user("hong@inha.edu"));

        recruitMemberService.updatePayment(application.getId(), true);

        assertThat(roleOf(account)).isEqualTo(UserRole.GUEST);
    }

    // 아직 가입하지 않은 지원자다. 올릴 대상이 없을 뿐 입금 처리 자체는 성공해야 한다.
    @Test
    void 계정이_없는_지원자의_입금_체크도_정상_처리된다() {
        RecruitMember application = recruitMemberRepository.save(member("12200001", "01000000001", "hong@inha.edu"));

        recruitMemberService.updatePayment(application.getId(), true);

        assertThat(recruitMemberRepository.findById(application.getId()).orElseThrow().getIsPayed()).isTrue();
    }

    private UserRole roleOf(User account) {
        return userRepository.findById(account.getId()).orElseThrow().getUserRole();
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
        return member(studentId, phoneNumber, email, currentSemester());
    }

    private RecruitMember member(
        String studentId,
        String phoneNumber,
        String email,
        AdmissionSemester semester
    ) {
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
            .admissionSemester(semester)
            .build();
    }

    /**
     * 테스트가 날짜에 묶이지 않도록 서비스와 같은 계산기를 쓴다.
     * 상수(Y26_2)를 박으면 학기가 넘어가는 순간 전부 무너진다.
     */
    private AdmissionSemester currentSemester() {
        return semesterCalculator.currentSemester();
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
