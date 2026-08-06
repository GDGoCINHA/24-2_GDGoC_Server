package inha.gdgoc.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 내 정보 엔드포인트의 인증 경계를 검증한다.
 *
 * <p>{@code /api/v1/users/**} 는 SecurityConfig 의 permitAll 목록에 없어야 하며
 * 기본 authenticated() 로 보호된다. 게시판 브랜치들이 같은 파일을 수정 중이라
 * 머지 과정에서 실수로 열리는 것을 이 테스트가 잡는다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMyProfile_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMyProfile_requiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김철수\",\"major\":\"CSE\",\"phoneNumber\":\"01012345678\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMyImage_requiresAuthentication() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/users/me/image")
                        .file(new MockMultipartFile(
                                "file", "avatar.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 공개 경로를 startsWith 로 등록하면 하위 경로까지 열린다. 이 리포에서
     * {@code /api/v1/board/events/*} 가 관리자용 {@code /deleted} 까지 공개해버린 전례가 있다.
     */
    @Test
    void unknownSubPath_isNotPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/anything"))
                .andExpect(status().isUnauthorized());
    }
}
