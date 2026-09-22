package inha.gdgoc.domain.eventapplication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * 로그인 없이 받는 행사 신청 경로의 인증 경계.
 *
 * <p>비로그인 경로 세 개만 열리고, 같은 행사의 계정용 경로와 관리자 경로는 계속 401 이어야 한다. 경로를 와일드카드로 열면 옆 경로까지 공개된다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AnonymousEventApplicationSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void anonymousForm_isPublic() throws Exception {
    // 폼이 없는 행사라 404 다. 401 이 아니라는 것이 요점이다.
    mockMvc
        .perform(get("/api/v1/board/events/999999/anonymous/form"))
        .andExpect(status().isNotFound());
  }

  @Test
  void anonymousApply_isPublic() throws Exception {
    // 빈 본문은 검증에서 400 으로 끝나 행이 생기지 않는다.
    mockMvc
        .perform(
            post("/api/v1/board/events/999999/anonymous/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void anonymousCheckin_isPublic() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/board/events/999999/anonymous/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void accountForm_stillRequiresLogin() throws Exception {
    mockMvc.perform(get("/api/v1/board/events/999999/form")).andExpect(status().isUnauthorized());
  }

  @Test
  void accountApply_stillRequiresLogin() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/board/events/999999/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void cancel_stillRequiresLogin() throws Exception {
    mockMvc
        .perform(delete("/api/v1/board/events/999999/applications/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminApplicants_stillRequiresLogin() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/events/999999/applications"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void anonymousPath_rejectsNonNumericId() throws Exception {
    mockMvc
        .perform(get("/api/v1/board/events/deleted/anonymous/form"))
        .andExpect(status().isUnauthorized());
  }
}
