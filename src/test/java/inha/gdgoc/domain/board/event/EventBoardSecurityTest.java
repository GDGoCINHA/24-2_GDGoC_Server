package inha.gdgoc.domain.board.event;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 행사 게시판 엔드포인트의 인증 경계를 검증한다.
 *
 * <p>공개 조회(GET)와 관리자 전용 API가 SecurityConfig 수준에서 올바르게 갈리는지 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class EventBoardSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deletedList_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/board/events/deleted"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/board/events"))
                .andExpect(status().isOk());
    }

    @Test
    void detail_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/board/events/999999"))
                .andExpect(status().isNotFound());
    }
}
