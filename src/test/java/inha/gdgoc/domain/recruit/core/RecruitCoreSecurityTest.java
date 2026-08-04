package inha.gdgoc.domain.recruit.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 코어 리크루팅 엔드포인트의 인증 경계를 검증한다.
 *
 * <p>기간 조회만 공개이고 나머지는 인증이 필요하다. 공개 경로는 SecurityConfig 와
 * TokenAuthenticationFilter 양쪽에 등록해야 하는데, 한쪽만 고치면 통과하지 못한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RecruitCoreSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void period_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/recruit/core/period"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void eligibility_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/recruit/core/eligibility"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prefill_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/recruit/core/prefill"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 공개 경로를 startsWith 로 등록하면 하위 경로까지 열린다. 이 리포에서
     * {@code /api/v1/board/events/*} 가 관리자용 {@code /deleted} 까지 공개해버린 전례가 있다.
     */
    @Test
    void periodSubPath_isNotPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/recruit/core/period/anything"))
                .andExpect(status().isUnauthorized());
    }
}
