package inha.gdgoc.domain.club;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 소모임 인증 경계. 목록·상세 조회만 비로그인에게 열려 있고, 나머지는 로그인을 요구한다.
 *
 * <p>비로그인이 단톡방 링크를 받지 않는 것은 {@code ClubServiceTest} 가 지킨다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ClubSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void list_isPublic() throws Exception {
    mockMvc.perform(get("/api/v1/clubs")).andExpect(status().isOk());
  }

  @Test
  void detail_isPublic() throws Exception {
    mockMvc.perform(get("/api/v1/clubs/999999")).andExpect(status().isNotFound());
  }

  @Test
  void myClubs_requiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/clubs/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void members_requireAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/clubs/1/members")).andExpect(status().isUnauthorized());
  }

  @Test
  void apply_requiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/v1/clubs/1/members")).andExpect(status().isUnauthorized());
  }

  @Test
  void leaderGrant_requiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/clubs/leader-grant/me")).andExpect(status().isUnauthorized());
  }
}
