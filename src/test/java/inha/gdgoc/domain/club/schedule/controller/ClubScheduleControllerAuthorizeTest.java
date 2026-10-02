package inha.gdgoc.domain.club.schedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import inha.gdgoc.domain.club.schedule.dto.request.ClubCheckinRequest;
import inha.gdgoc.domain.club.schedule.dto.request.ClubRsvpRequest;
import inha.gdgoc.domain.club.schedule.dto.request.ClubScheduleRequest;
import inha.gdgoc.domain.club.schedule.enums.ClubRsvp;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import java.time.Instant;
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
 * 일정·참석 응답·QR 체크인 API 의 역할 경계. GUEST 는 7개 엔드포인트 모두 403 이어야 한다.
 *
 * <p>spring-security-test 가 없어 MockMvc 대신 프록시된 컨트롤러 빈을 직접 부른다({@code
 * ClubActivityControllerAuthorizeTest} 와 같은 방식).
 */
@ActiveProfiles("test")
@SpringBootTest
class ClubScheduleControllerAuthorizeTest {

  private static final ClubScheduleRequest SCHEDULE =
      new ClubScheduleRequest("모임", Instant.parse("2026-10-07T10:00:00Z"), null, null, null);

  @Autowired private ClubScheduleController controller;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("GUEST 는 일정·참석 응답·QR API 전부 403")
  void guestIsForbiddenEverywhere() {
    CustomUserDetails guest = loginAs(UserRole.GUEST);

    assertThatThrownBy(() -> controller.list(guest, 1L, "upcoming"))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.create(guest, 1L, SCHEDULE))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.update(guest, 1L, 1L, SCHEDULE))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.delete(guest, 1L, 1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.rsvp(guest, 1L, new ClubRsvpRequest(ClubRsvp.ATTEND)))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.checkinToken(guest, 1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.checkIn(guest, new ClubCheckinRequest("a.b.c")))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("MEMBER 는 역할 검사를 통과한다 (그다음은 서비스의 리더·멤버 검사)")
  void memberPassesRoleCheck() {
    CustomUserDetails member = loginAs(UserRole.MEMBER);

    Throwable thrown = catchThrowable(() -> controller.list(member, -1L, "upcoming"));

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
