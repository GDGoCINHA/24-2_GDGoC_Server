package inha.gdgoc.domain.club.completion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 기수·목표·쉬는 주·완주 엔드포인트의 인증 경계. 소모임 API 는 전부 로그인 필요다 — permitAll 에 들어가면 안 된다.
 *
 * <p>여기서는 비로그인 차단까지 본다. MEMBER·CORE 경계는 {@code @Authorize} 가, 리더·운영진 경계는 {@code
 * ClubCompletionServiceTest} 가 맡는다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ClubCompletionSecurityTest {

  @Autowired private MockMvc mockMvc;

  private void expect401(MockHttpServletRequestBuilder req) throws Exception {
    mockMvc
        .perform(req.contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void terms_requireAuthentication() throws Exception {
    expect401(get("/api/v1/club-terms"));
    expect401(post("/api/v1/admin/club-terms"));
    expect401(patch("/api/v1/admin/club-terms/1"));
  }

  @Test
  void completion_requiresAuthentication() throws Exception {
    expect401(get("/api/v1/clubs/1/completion"));
    expect401(post("/api/v1/admin/clubs/1/completion"));
  }

  @Test
  void reviewAndAdminStatus_requireAuthentication() throws Exception {
    expect401(get("/api/v1/admin/club-activities"));
    expect401(post("/api/v1/admin/club-activities/1/approve"));
    expect401(post("/api/v1/admin/club-activities/1/request-revision"));
    expect401(get("/api/v1/admin/clubs"));
    expect401(get("/api/v1/admin/clubs/export"));
  }

  @Test
  void reactions_requireAuthentication() throws Exception {
    expect401(put("/api/v1/club-reactions/POST/1/like"));
    expect401(delete("/api/v1/club-reactions/POST/1/like"));
    expect401(get("/api/v1/club-reactions/ACTIVITY/1/comments"));
    expect401(post("/api/v1/club-reactions/ACTIVITY/1/comments"));
    expect401(delete("/api/v1/club-comments/1"));
  }

  @Test
  void goalAndRestWeeks_requireAuthentication() throws Exception {
    expect401(put("/api/v1/clubs/1/rest-weeks"));
    expect401(put("/api/v1/clubs/1/goal"));
    expect401(put("/api/v1/clubs/1/goal-result"));
    expect401(post("/api/v1/admin/clubs/1/goal-status"));
  }
}
