package inha.gdgoc.domain.board.free;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 자유게시판 엔드포인트의 인증 경계를 검증한다.
 *
 * <p>자유게시판은 회원 전용이다. 공개인 게시판은 행사 하나뿐이므로 SecurityConfig 의 permitAll 에
 * {@code /api/v1/board/free} 가 들어가면 안 된다. 이 테스트가 그 실수를 잡는다 — 공지에서 실제로 한 번
 * 났던 사고다.
 *
 * <p>여기서 검증하는 것은 '비로그인 차단'까지다. 작성 MEMBER 이상, 수정·삭제 본인 또는 ORGANIZER 이상은
 * 각각 {@code @Authorize} 와 {@code FreeBoardService} 가 담당하며 이 테스트의 범위가 아니다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class FreeBoardSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void list_requiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/board/free")).andExpect(status().isUnauthorized());
  }

  @Test
  void detail_requiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/board/free/999999")).andExpect(status().isUnauthorized());
  }

  @Test
  void create_requiresAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/board/free")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"content\":\"c\"}"))
        .andExpect(status().isUnauthorized());
  }
}
