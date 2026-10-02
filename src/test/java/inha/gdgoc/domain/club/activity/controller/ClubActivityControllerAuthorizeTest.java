package inha.gdgoc.domain.club.activity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import inha.gdgoc.domain.club.activity.dto.request.ClubActivitySubmitRequest;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import java.time.LocalDate;
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
 * 활동 기록 API 의 역할 경계. GUEST 는 소모임에 참여할 수 없으므로 모든 엔드포인트에서 403 이어야 한다.
 *
 * <p>비로그인(401)은 SecurityConfig 의 {@code anyRequest().authenticated()} 가 막는다. 여기서는 그다음 단계인
 * {@code @Authorize} 가 컨트롤러 전체에 걸려 있는지를 본다 — 메서드 하나에 빠뜨리면 GUEST 가 그 API 를 쓴다. 이 리포에는
 * spring-security-test 가 없어서 MockMvc 대신 프록시된 컨트롤러 빈을 직접 부른다.
 */
@ActiveProfiles("test")
@SpringBootTest
class ClubActivityControllerAuthorizeTest {

  private static final ClubActivitySubmitRequest REQ =
      new ClubActivitySubmitRequest(
          LocalDate.of(2026, 10, 7), null, List.of("a.jpg"), "내용", List.of(), null);

  @Autowired private ClubActivityController controller;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("GUEST 는 활동 기록 API 전부 403")
  void guestIsForbiddenEverywhere() {
    CustomUserDetails guest = loginAs(UserRole.GUEST);

    assertThatThrownBy(() -> controller.roster(guest, 1L, LocalDate.of(2026, 10, 7), null))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.create(guest, 1L, REQ))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.detail(guest, 1L, 1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.update(guest, 1L, 1L, REQ))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("MEMBER 는 역할 검사를 통과한다 (그다음은 서비스의 리더·멤버 검사)")
  void memberPassesRoleCheck() {
    CustomUserDetails member = loginAs(UserRole.MEMBER);

    Throwable thrown = catchThrowable(() -> controller.detail(member, -1L, -1L));

    assertThat(thrown).isNotInstanceOf(AccessDeniedException.class);
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
