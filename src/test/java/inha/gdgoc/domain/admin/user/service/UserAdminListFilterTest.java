package inha.gdgoc.domain.admin.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.admin.user.dto.response.UserSummaryResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
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
 * 관리자 유저 목록의 권한·팀 필터와 검색 범위를 실제 조회로 검증한다.
 *
 * <p>전에는 이름 검색뿐이라 관리 화면이 20행씩 넘기며 사람을 찾아야 했다.
 * 검색이 이메일·학번까지 훑는지도 여기서 잡는다 — 이름만 훑던 시절로 돌아가면
 * 화면의 placeholder 가 거짓말이 된다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserAdminListFilterTest {

    private static final Pageable PAGE = PageRequest.of(0, 20);

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(user("김가나", "12200001", "01000000001", "gana@inha.edu", UserRole.CORE, TeamType.TECH));
        userRepository.save(user("이다라", "12200002", "01000000002", "dara@inha.edu", UserRole.CORE, TeamType.HR));
        userRepository.save(user("박마바", "12200003", "01000000003", "maba@inha.edu", UserRole.MEMBER, null));
    }

    @Test
    void 권한으로_거르면_그_권한만_나온다() {
        Page<UserSummaryResponse> result = userAdminService.listUsers(null, UserRole.CORE, null, PAGE);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(u -> u.userRole() == UserRole.CORE);
    }

    @Test
    void 팀으로_거르면_그_팀만_나온다() {
        Page<UserSummaryResponse> result = userAdminService.listUsers(null, null, TeamType.TECH, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("김가나");
    }

    @Test
    void 권한과_팀을_함께_걸면_둘_다_만족하는_유저만_나온다() {
        Page<UserSummaryResponse> result = userAdminService.listUsers(null, UserRole.CORE, TeamType.HR, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("이다라");
    }

    @Test
    void 필터가_없으면_전체가_나온다() {
        Page<UserSummaryResponse> result = userAdminService.listUsers(null, null, null, PAGE);

        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    void 검색어가_이메일에도_걸린다() {
        Page<UserSummaryResponse> result = userAdminService.listUsers("dara@", null, null, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("이다라");
    }

    @Test
    void 검색어가_학번에도_걸린다() {
        Page<UserSummaryResponse> result = userAdminService.listUsers("12200003", null, null, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("박마바");
    }

    @Test
    void 이름_검색은_그대로_동작한다() {
        Page<UserSummaryResponse> result = userAdminService.listUsers("가나", null, null, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("김가나");
    }

    private User user(String name, String studentId, String phoneNumber, String email,
                      UserRole role, TeamType team) {
        return User.builder()
                .name(name)
                .oauthSubject("oauth-" + studentId)
                .major("컴퓨터공학과")
                .studentId(studentId)
                .phoneNumber(phoneNumber)
                .email(email)
                .userRole(role)
                .team(team)
                .image(null)
                .social(null)
                .careers(null)
                .build();
    }
}
