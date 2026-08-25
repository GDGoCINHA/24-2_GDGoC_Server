package inha.gdgoc.domain.landing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/**
 * 온보딩 콘텐츠의 인증 경계.
 *
 * <p>방문자가 읽는 경로 하나만 공개다. 편집·발행과 모집 기간 변경은 전부 막혀 있어야 한다 — 특히 모집
 * 기간은 지원 창구를 여닫는 값이라 새어 나가면 지원을 임의로 열고 닫을 수 있다.
 *
 * <p>여기서는 비로그인만 검증한다. LEAD 미만 로그인 사용자에 대한 거절은 {@code AccessGuard} 가
 * 공통으로 처리하고 다른 관리자 엔드포인트와 같은 경로를 지난다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class LandingContentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicRead_isOpen() throws Exception {
        mockMvc.perform(get("/api/v1/landing-content")).andExpect(status().isOk());
    }

    @Test
    void draftRead_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/landing-content"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void draftSave_requiresAuthentication() throws Exception {
        mockMvc.perform(
                put("/api/v1/admin/landing-content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void publish_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/landing-content/publish"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void recruitPeriodRead_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/recruit/CORE/period"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void recruitPeriodUpdate_requiresAuthentication() throws Exception {
        mockMvc.perform(
                put("/api/v1/admin/recruit/MEMBER/period")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"openAt\":\"2026-10-01T00:00:00Z\",\"closeAt\":\"2026-10-31T00:00:00Z\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void recruitPeriodClear_requiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/recruit/MEMBER/period"))
            .andExpect(status().isUnauthorized());
    }
}
