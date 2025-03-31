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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

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
        Claims claims = tokenProvider.validToken(refreshToken);
        try {
            Long userId = Long.parseLong(claims.getSubject());
            Optional<User> optionalUser = userRepository.findById(userId);

            if (optionalUser.isEmpty()) {
                throw new RuntimeException("User not found");
            }
            User user = optionalUser.get();

            Optional<RefreshToken> refreshTokenEntity = refreshTokenRepository.findByUser(user);
            if (refreshTokenEntity.isEmpty() || !refreshTokenEntity.get().getToken().equals(refreshToken)) {
                throw new RuntimeException("Invalid Refresh Token");
            }

            return tokenProvider.generateGoogleLoginToken(user, Duration.ofHours(1));
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
