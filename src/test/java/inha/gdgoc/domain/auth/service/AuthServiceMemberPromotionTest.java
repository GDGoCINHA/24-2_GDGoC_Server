package inha.gdgoc.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import inha.gdgoc.domain.auth.dto.request.SignupRequest;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.recruit.member.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.member.enums.Gender;
import inha.gdgoc.domain.recruit.member.repository.RecruitMemberRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.util.SemesterCalculator;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회비를 낸 부원 지원자가 가입·로그인하는 순간 MEMBER 가 되는지 검증한다.
 *
 * <p>부원 지원은 비로그인으로 받아 지원서에 계정을 가리키는 컬럼이 없다. 지원 시점에는 올릴 대상이
 * 없으므로 가입·로그인이 유일한 승격 지점이고, 이메일이 둘을 잇는 키다. 이 테스트가 깨지면
 * 지원자는 회비를 내고도 GUEST 로 남아 자유게시판에 글을 쓰지 못한다.
 *
 * <p>login() 은 구글 토큰 검증을 타므로 여기서는 signup() 으로 검증한다. 두 경로가 같은
 * promotePaidApplicantToMember 를 부른다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthServiceMemberPromotionTest {

    /** 실행 시점이 언제든 반드시 과거인 학기. enum 의 첫 상수라 현재 학기가 될 일이 없다. */
    private static final AdmissionSemester PAST_SEMESTER = AdmissionSemester.Y21_2;

    @Autowired
    private AuthService authService;

    @Autowired
    private RecruitMemberRepository recruitMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SemesterCalculator semesterCalculator;

    /** 가입은 리프레시 토큰을 Redis 에 넣는다. 테스트 환경에는 Redis 가 없어 대체한다. */
    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        recruitMemberRepository.deleteAll();

        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        Mockito.doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void 회비를_낸_이번_학기_지원자는_가입하면_MEMBER_가_된다() {
        recruitMemberRepository.save(application("hong@inha.edu", currentSemester(), true));

        authService.signup(signupRequest("hong@inha.edu"));

        assertThat(roleOf("hong@inha.edu")).isEqualTo(UserRole.MEMBER);
    }

    // 입금 확인은 운영진이 나중에 한다. 그 전에 가입하면 아직 GUEST 여야 한다.
    @Test
    void 회비를_내지_않은_지원자는_GUEST_로_남는다() {
        recruitMemberRepository.save(application("hong@inha.edu", currentSemester(), false));

        authService.signup(signupRequest("hong@inha.edu"));

        assertThat(roleOf("hong@inha.edu")).isEqualTo(UserRole.GUEST);
    }

    // 지난 학기 지원서로 이번 학기 권한을 얻으면 안 된다. 회비는 학기마다 받는다.
    @Test
    void 지난_학기_지원서로는_승격되지_않는다() {
        recruitMemberRepository.save(application("hong@inha.edu", PAST_SEMESTER, true));

        authService.signup(signupRequest("hong@inha.edu"));

        assertThat(roleOf("hong@inha.edu")).isEqualTo(UserRole.GUEST);
    }

    @Test
    void 지원_이력이_없으면_GUEST_로_남는다() {
        authService.signup(signupRequest("hong@inha.edu"));

        assertThat(roleOf("hong@inha.edu")).isEqualTo(UserRole.GUEST);
    }

    // 남의 지원서로 올라가면 안 된다. 이메일이 다르면 없는 것으로 본다.
    @Test
    void 다른_사람의_지원서로는_승격되지_않는다() {
        recruitMemberRepository.save(application("kim@inha.edu", currentSemester(), true));

        authService.signup(signupRequest("hong@inha.edu"));

        assertThat(roleOf("hong@inha.edu")).isEqualTo(UserRole.GUEST);
    }

    // 지원 폼과 구글 계정의 대소문자가 다를 수 있다. 그걸로 승격이 갈리면 안 된다.
    @Test
    void 이메일_대소문자가_달라도_승격된다() {
        recruitMemberRepository.save(application("Hong@Inha.edu", currentSemester(), true));

        authService.signup(signupRequest("hong@inha.edu"));

        assertThat(roleOf("hong@inha.edu")).isEqualTo(UserRole.MEMBER);
    }

    private UserRole roleOf(String email) {
        return userRepository.findByOauthSubject("oauth-" + email)
            .map(User::getUserRole)
            .orElseThrow(() -> new AssertionError("가입한 사용자를 찾을 수 없습니다: " + email));
    }

    private SignupRequest signupRequest(String email) {
        SignupRequest request = new SignupRequest();
        request.setOauthSubject("oauth-" + email);
        request.setEmail(email);
        request.setName("홍길동");
        request.setStudentId("12200001");
        request.setMajor("컴퓨터공학과");
        request.setPhoneNumber("01000000001");
        return request;
    }

    private RecruitMember application(String email, AdmissionSemester semester, boolean payed) {
        return RecruitMember.builder()
            .name("홍길동")
            .studentId("12200001")
            .enrolledClassification(EnrolledClassification.FULL_REGISTRATION)
            .phoneNumber("01000000001")
            .email(email)
            .gender(Gender.PRIVATE)
            .birth(LocalDate.of(2003, 3, 1))
            .major("CSE")
            .isPayed(payed)
            .admissionSemester(semester)
            .build();
    }

    /**
     * 테스트가 날짜에 묶이지 않도록 서비스와 같은 계산기를 쓴다.
     * 상수를 박으면 학기가 넘어가는 순간 전부 무너진다.
     */
    private AdmissionSemester currentSemester() {
        return semesterCalculator.currentSemester();
    }
}
