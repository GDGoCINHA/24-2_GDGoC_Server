package inha.gdgoc.domain.board.notice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 공지 게시판 엔드포인트의 인증 경계를 검증한다.
 *
 * <p>공지는 회원 전용이다. 행사 게시판과 달리 목록·상세 조회에도 인증이 필요하다.
 * SecurityConfig 에 GET permitAll 이 들어 있던 적이 있어 비로그인에게도 열려 있었고,
 * 프론트의 로그인 게이트만으로는 API 직접 호출을 막지 못했다.
 *
 * <p>행사 게시판이 공개로 남아 있는지는 {@code EventBoardSecurityTest} 가 지킨다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class NoticeBoardSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void list_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/board/notices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detail_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/board/notices/999999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pinnedList_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/board/notices/pinned"))
                .andExpect(status().isUnauthorized());
    }
}
