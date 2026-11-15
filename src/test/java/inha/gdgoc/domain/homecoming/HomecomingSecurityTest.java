package inha.gdgoc.domain.homecoming;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * 홈커밍데이 신청의 인증 경계. 제출 POST 하나만 공개이고, 같은 경로의 GET 과 관리 경로는 계속 401 이어야 한다 — 전화번호가 담긴 명단이다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class HomecomingSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void submit_isPublic() throws Exception {
    // 빈 본문은 검증에서 400 으로 끝나 행이 생기지 않는다. 401 이 아니라는 것이 요점이다.
    mockMvc
        .perform(post("/api/v1/homecoming/applications").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listingApplications_onPublicPath_requiresLogin() throws Exception {
    mockMvc.perform(get("/api/v1/homecoming/applications")).andExpect(status().isUnauthorized());
  }

  @Test
  void adminList_requiresLogin() throws Exception {
    mockMvc.perform(get("/api/v1/admin/homecoming/applications")).andExpect(status().isUnauthorized());
  }

  @Test
  void adminExport_requiresLogin() throws Exception {
    mockMvc.perform(get("/api/v1/admin/homecoming/export/nametags")).andExpect(status().isUnauthorized());
  }

  @Test
  void adminUpdate_requiresLogin() throws Exception {
    mockMvc
        .perform(patch("/api/v1/admin/homecoming/applications/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }
}
