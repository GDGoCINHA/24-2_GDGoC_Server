package inha.gdgoc.domain.club.activity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import inha.gdgoc.domain.club.activity.dto.request.ClubFixRequestCreateRequest;
import inha.gdgoc.domain.club.activity.enums.ClubFixRequestStatus;
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
 * 내 출석·출석 수정 요청 API 의 역할 경계. GUEST 는 5개 엔드포인트 모두 403 이어야 한다.
 *
 * <p>spring-security-test 가 없어 MockMvc 대신 프록시된 컨트롤러 빈을 직접 부른다({@code
 * ClubActivityControllerAuthorizeTest} 와 같은 방식).
 */
@ActiveProfiles("test")
@SpringBootTest
class ClubAttendanceControllerAuthorizeTest {

  private static final ClubFixRequestCreateRequest REQ = new ClubFixRequestCreateRequest("사유");

  @Autowired private ClubAttendanceController controller;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("GUEST 는 내 출석·수정 요청 API 전부 403")
  void guestIsForbiddenEverywhere() {
    CustomUserDetails guest = loginAs(UserRole.GUEST);

    assertThatThrownBy(() -> controller.myAttendance(guest, 1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.requestFix(guest, 1L, REQ))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.list(guest, 1L, ClubFixRequestStatus.PENDING))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.accept(guest, 1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.reject(guest, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("MEMBER 는 역할 검사를 통과한다 (그다음은 서비스의 명단·리더 검사)")
  void memberPassesRoleCheck() {
    CustomUserDetails member = loginAs(UserRole.MEMBER);

    Throwable thrown = catchThrowable(() -> controller.myAttendance(member, -1L));

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
