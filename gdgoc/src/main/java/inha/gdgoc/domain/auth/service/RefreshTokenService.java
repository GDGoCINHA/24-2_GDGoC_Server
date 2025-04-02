package inha.gdgoc.domain.auth.service;

import inha.gdgoc.config.jwt.TokenProvider;
import inha.gdgoc.domain.auth.entity.RefreshToken;
import inha.gdgoc.domain.auth.repository.RefreshTokenRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public String getOrCreateRefreshToken(User user, Duration duration) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        // 유효한 토큰이 있으면 재사용
        if (existingToken.isPresent() && existingToken.get().getExpiryDate().isAfter(LocalDateTime.now())) {
            return existingToken.get().getToken();
        }

        // 없거나 만료되었으면 새로 생성
        String newToken = tokenProvider.generateGoogleLoginToken(user, duration);
        saveRefreshToken(newToken, user, duration);
        return newToken;
    }

    // 로그인 시 refresh 토큰 저장
    public void saveRefreshToken(String refreshToken, User user, Duration expiredAt) {
        LocalDateTime expiryDate = LocalDateTime.now().plus(expiredAt);

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);
        if(existingToken.isPresent()) {
            existingToken.get().update(refreshToken, expiryDate);
            return;
        }

        RefreshToken tokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(expiryDate)
                .build();

        refreshTokenRepository.save(tokenEntity);
    }

    // 새로운 access token 발급
    public String refreshAccessToken(String refreshToken) {
        log.info("리프레시 토큰 서비스 호출됨. 토큰: {}", refreshToken);

        Claims claims = tokenProvider.validToken(refreshToken);
        try {
            String email = claims.getSubject(); // 이메일로 변경
            Optional<User> optionalUser = userRepository.findByEmail(email);

            if (optionalUser.isEmpty()) {
                throw new RuntimeException("User not found");
            }
            User user = optionalUser.get();

            Optional<RefreshToken> refreshTokenEntity = refreshTokenRepository.findByUser(user);
            if(refreshTokenEntity.isEmpty()) {
                log.info("없어요");
            }
            if (refreshTokenEntity.isEmpty() || !refreshTokenEntity.get().getToken().trim().equals(refreshToken.trim())) {
                log.info("비어있나요 :{}",refreshTokenEntity.isEmpty());
                log.info("parameter:{}", refreshToken.trim());
                log.info("기존에 있던 값:{}", refreshTokenEntity.get().getToken().trim());
                log.info("같은가요:{}",refreshTokenEntity.get().getToken().trim().equals(refreshToken));
                throw new RuntimeException("Invalid Refresh Token");
            }

            return tokenProvider.generateGoogleLoginToken(user, Duration.ofHours(1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
