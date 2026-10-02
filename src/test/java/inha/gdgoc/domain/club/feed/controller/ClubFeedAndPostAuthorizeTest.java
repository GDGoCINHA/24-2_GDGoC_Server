package inha.gdgoc.domain.club.feed.controller;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.club.post.controller.ClubPostController;
import inha.gdgoc.domain.club.post.dto.request.ClubPostRequest;
import inha.gdgoc.domain.club.post.enums.ClubPostCategory;
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
 * 피드·게시글 API 의 역할 경계. 활동 피드는 GUEST 에게 보이지 않아야 하고, 게시글도 쓸 수 없어야 한다.
 *
 * <p>비로그인(401)은 SecurityConfig 의 {@code anyRequest().authenticated()} 가 막는다 — 피드 경로를 permitAll 에 넣지
 * 않았다. spring-security-test 가 없어 프록시된 컨트롤러 빈을 직접 부른다.
 */
@ActiveProfiles("test")
@SpringBootTest
class ClubFeedAndPostAuthorizeTest {

  private static final ClubPostRequest REQ =
      new ClubPostRequest(ClubPostCategory.QUESTION, "내용", List.of());

  @Autowired private ClubFeedController feedController;
  @Autowired private ClubPostController postController;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("GUEST 는 피드·게시글 API 전부 403")
  void guestIsForbiddenEverywhere() {
    CustomUserDetails guest = loginAs(UserRole.GUEST);

    assertThatThrownBy(() -> feedController.teamFeed(guest, 1L, 0, 20))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> feedController.globalFeed(guest, 0, 20))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> postController.create(guest, 1L, REQ))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> postController.update(guest, 1L, 1L, REQ))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> postController.delete(guest, 1L, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("MEMBER 는 역할 검사를 통과한다 — 전체 피드는 소모임 권한이 없어 그대로 열린다")
  void memberPassesRoleCheck() {
    CustomUserDetails member = loginAs(UserRole.MEMBER);

    assertThatCode(() -> feedController.globalFeed(member, 0, 20)).doesNotThrowAnyException();
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
