package inha.gdgoc.domain.club.club.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * 소모임 삭제의 역할 경계. 운영진 삭제는 공개된 소모임까지 지우므로 MEMBER 가 부르면 403 이어야 한다.
 *
 * <p>spring-security-test 가 없어 프록시된 컨트롤러 빈을 직접 부른다({@code ClubActivityControllerAuthorizeTest} 와 같다).
 */
@ActiveProfiles("test")
@SpringBootTest
class ClubDeleteAuthorizeTest {

  @Autowired private ClubController clubController;
  @Autowired private ClubAdminController clubAdminController;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("MEMBER 는 운영진 삭제 API 를 부를 수 없다")
  void memberCannotUseStaffDelete() {
    loginAs(UserRole.MEMBER);

    assertThatThrownBy(() -> clubAdminController.delete(-1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("CORE 는 운영진 삭제의 역할 검사를 통과한다")
  void corePassesStaffDelete() {
    loginAs(UserRole.CORE);

    assertThat(catchThrowable(() -> clubAdminController.delete(-1L)))
        .isNotInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("GUEST 는 리더 삭제 API 도 부를 수 없다")
  void guestCannotUseLeaderDelete() {
    CustomUserDetails guest = loginAs(UserRole.GUEST);

    assertThatThrownBy(() -> clubController.delete(guest, -1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  private static CustomUserDetails loginAs(UserRole role) {
    CustomUserDetails user =
        new CustomUserDetails(
            1L,
            "user",
            "session",
            List.of(new SimpleGrantedAuthority("ROLE_" + role.name())),
            role,
            null);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    return user;
  }
}
