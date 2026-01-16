package inha.gdgoc.domain.auth.service;

import inha.gdgoc.domain.auth.dto.response.LoginResponse;
import inha.gdgoc.domain.auth.enums.LoginType;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.config.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static inha.gdgoc.global.util.EncryptUtil.encrypt;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate; 
    


    @Value("${app.google.client-id}")
    private String googleClientId;

    //로그인
    @Transactional
    public Object login(String idToken) {
        //  Google ID Token 검증
        GoogleUserInfo googleUser = verifyGoogleToken(idToken);

        // 도메인 검증 (인하대 메일만 허용)
        if (!googleUser.getEmail().endsWith("@inha.edu")) {
            throw new IllegalArgumentException("인하대학교(@inha.edu) 계정만 이용 가능합니다.");
        }

        //  DB에서 유저 조회 (OAuth Subject 기준)
        User user = userRepository.findByOauthSubject(googleUser.getSub()).orElse(null);

        // 신규 유저 -> 회원가입 필요 응답 (202 or 200 with isNewUser=true)
        if (user == null) {
            return SignupNeededResponse.builder()
                    .isNewUser(true)
                    .oauthSubject(googleUser.getSub())
                    .email(googleUser.getEmail())
                    .name(googleUser.getName())
                    .build();
        }

        // 기존 유저 -> 토큰 발급 및 로그인 성공 응답
        TokenDto tokens = generateTokens(user);
        return LoginSuccessResponse.of(user, tokens);
    }

    //회원가입
    @Transactional
    public LoginSuccessResponse signup(SignupRequest request) {
        // 학번 중복 체크
        if (userRepository.existsByStudentId(request.getStudentId())) {
            throw new IllegalArgumentException("이미 존재하는 학번입니다.");
        }

        // 전화번호 정규화 (숫자만 남김)
        String cleanPhone = request.getPhoneNumber().replaceAll("[^0-9]", "");

        // 유저 엔티티 생성 및 저장
        User newUser = User.builder()
                .oauthSubject(request.getOauthSubject()) // 구글 sub
                .email(request.getEmail())
                .name(request.getName())
                .studentId(request.getStudentId())
                .major(request.getMajor())
                .phoneNumber(cleanPhone)
                // Role(GUEST), Status(PENDING) 등은 User 엔티티 생성자에서 기본값 처리됨
                .build();

        userRepository.save(newUser);

        // 토큰 발급
        TokenDto tokens = generateTokens(newUser);
        return LoginSuccessResponse.of(newUser, tokens);
    }
    public String refresh(String refreshToken) {
        // Redis에서 Refresh Token 확인
        String redisKey = "RT:" + refreshToken;
        String subject = redisTemplate.opsForValue().get(redisKey);

        if (subject == null) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }

        // DB에서 유저 조회 (권한 변경 등이 있었을 수 있으므로 다시 조회)
        User user = userRepository.findByOauthSubject(subject)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // Access Token만 새로 발급 (Refresh Token은 그대로 유지하거나, 정책에 따라 재발급 가능)
        return tokenProvider.createAccessToken(user);
    }
    public void logout(String refreshToken) {
        // Redis에서 Refresh Token 삭제
        String redisKey = "RT:" + refreshToken;
        redisTemplate.delete(redisKey);
    }

    
     //토큰 발급 및 Redis 저장
     
    private TokenDto generateTokens(User user) {
        // Access Token 생성 (JWT)
        String accessToken = tokenProvider.createAccessToken(user);
        
        // Refresh Token 생성 (Random UUID)
        String refreshToken = tokenProvider.createRefreshToken();

        // Redis 저장 (Key: "RT:{refreshToken}", Value: oauthSubject, 유효기간: 14일)
        redisTemplate.opsForValue().set(
                "RT:" + refreshToken,
                user.getOauthSubject(),
                14,
                TimeUnit.DAYS
        );

        return new TokenDto(accessToken, refreshToken);
    }

    
    // Google ID Token 검증
    private GoogleUserInfo verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                return new GoogleUserInfo(
                        payload.getSubject(),        // sub
                        payload.getEmail(),          // email
                        (String) payload.get("name") // name
                );
            } else {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("Google Token Verification Failed", e);
            throw new IllegalArgumentException("토큰 검증 실패", e);
        }
    }
}

