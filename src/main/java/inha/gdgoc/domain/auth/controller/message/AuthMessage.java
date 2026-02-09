package inha.gdgoc.domain.auth.controller.message;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthMessage {
    public static final String LOGIN_SUCCESS = "로그인에 성공하였습니다.";
    public static final String SIGNUP_SUCCESS = "회원가입에 성공하였습니다.";
    public static final String ACCESS_TOKEN_REFRESH_SUCCESS = "토큰 재발급에 성공하였습니다.";
    public static final String LOGOUT_SUCCESS = "로그아웃에 성공하였습니다.";
    public static final String OAUTH_LOGIN_SIGNUP_SUCCESS = "OAuth 로그인/회원가입에 성공하였습니다.";
    public static final String STUDENT_ID_DUPLICATION_CHECK_SUCCESS = "학번 중복 확인에 성공하였습니다.";
    public static final String PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS = "전화번호 중복 확인에 성공하였습니다.";
}
