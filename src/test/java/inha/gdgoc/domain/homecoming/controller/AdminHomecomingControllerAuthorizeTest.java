package inha.gdgoc.domain.homecoming.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * 관리 API 는 회장(ORGANIZER 이상)과 HR 팀 코어 이상만 쓴다. 다른 팀 코어·리드는 막혀야 한다.
 *
 * <p>이 리포에는 spring-security-test 가 없어 프록시된 컨트롤러 빈을 직접 부른다.
 */
@ActiveProfiles("test")
@SpringBootTest
class AdminHomecomingControllerAuthorizeTest {

  @Autowired private AdminHomecomingController controller;

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest(name = "{0}/{1} 는 403")
  @CsvSource({"GUEST,", "MEMBER,HR", "CORE,TECH", "CORE,BD", "LEAD,PR_DESIGN", "LEAD,"})
  @DisplayName("회장·HR 이 아니면 목록·집계·내보내기·수정·취소 모두 403")
  void forbidden(UserRole role, TeamType team) {
    CustomUserDetails me = loginAs(role, team);

    assertThatThrownBy(() -> controller.list(null, null)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.get(1L)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.summary()).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.exportNametags()).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.exportApplications()).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.update(me, 1L, null)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.cancel(me, 1L, null)).isInstanceOf(AccessDeniedException.class);
  }

  @ParameterizedTest(name = "{0}/{1} 는 통과")
  @CsvSource({"CORE,HR", "LEAD,HR", "ORGANIZER,", "ADMIN,"})
  @DisplayName("회장·HR 은 역할 검사를 통과한다")
  void allowed(UserRole role, TeamType team) {
    loginAs(role, team);

    assertThat(controller.summary().getStatusCode().is2xxSuccessful()).isTrue();
  }

  private static CustomUserDetails loginAs(UserRole role, TeamType team) {
    CustomUserDetails user =
        new CustomUserDetails(
            1L, "user", "session", List.of(new SimpleGrantedAuthority("ROLE_" + role.name())), role, team);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    return user;
  }
}
