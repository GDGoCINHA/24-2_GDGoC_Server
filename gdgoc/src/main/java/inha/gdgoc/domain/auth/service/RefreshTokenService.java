package inha.gdgoc.domain.auth.service;

import inha.gdgoc.config.jwt.TokenProvider;
import inha.gdgoc.domain.auth.entity.RefreshToken;
import inha.gdgoc.domain.auth.repository.RefreshTokenRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String getOrCreateRefreshToken(User user, Duration duration) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        // 1. 유효한 토큰이 있으면 재사용
        if (existingToken.isPresent()) {
            RefreshToken refreshToken = existingToken.get();

            // 로컬 시간 기준으로 만료 시간 체크
            if (refreshToken.getExpiryDate().isAfter(LocalDateTime.now())) {
                log.info("유효한 Refresh Token이 존재합니다. 재사용합니다: {}", refreshToken.getToken());
                return refreshToken.getToken();
            }
        }

        // 2. 없거나 만료되었으면 새로 생성
        String newToken = tokenProvider.generateRefreshToken(user, duration);
        log.info("새로운 Refresh Token 생성됨: {}", newToken);

        // 3. 토큰 저장 (Private 메서드 활용)
        saveRefreshToken(newToken, user, duration);

        return newToken;
    }

    @Transactional
    public String refreshAccessToken(String refreshToken) {
        log.info("리프레시 토큰 서비스 호출됨. 토큰: {}", refreshToken);

        // 1. JWT 파싱하여 이메일 추출
        Claims claims = tokenProvider.validToken(refreshToken);
        if (claims == null) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        String email = claims.getSubject();
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("해당 이메일로 등록된 유저를 찾을 수 없습니다.");
        }

        User user = optionalUser.get();

        // 2. DB에서 RefreshToken 조회
        Optional<RefreshToken> refreshTokenEntity = refreshTokenRepository.findByUser(user);

        if (refreshTokenEntity.isEmpty()) {
            throw new RuntimeException("DB에 저장된 리프레시 토큰이 없습니다.");
        }

        RefreshToken storedToken = refreshTokenEntity.get();

        // 만료 시간 체크 (로컬 시간 기준)
        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("리프레시 토큰이 만료되었습니다.");
        }

        if (!storedToken.getToken().equals(refreshToken)) {
            log.info("DB에 저장된 토큰: {}", storedToken.getToken());
            throw new RuntimeException("리프레시 토큰이 일치하지 않습니다.");
        }

        // 3. AccessToken 새로 발급
        String newAccessToken = tokenProvider.generateGoogleLoginToken(user, Duration.ofHours(1));
        log.info("새로운 AccessToken 생성됨: {}", newAccessToken);

        return newAccessToken;
    }

    @Transactional
    public boolean logout(Long userId) {
        try {
            Optional<RefreshToken> tokenEntity = refreshTokenRepository.findByUserId(userId);
            if (tokenEntity.isPresent()) {
                log.info("사용자 ID: {}에 대한 토큰을 DB에서 찾았습니다. 토큰 ID: {}", userId, tokenEntity.get().getId());
            } else {
                log.warn("사용자 ID: {}에 대한 토큰이 DB에 존재하지 않습니다.", userId);
                return false;
            }

            // 토큰 삭제 실행 및 삭제된 행 수 확인
            refreshTokenRepository.deleteByUserId(userId);
            log.info("사용자 ID: {} 로그아웃 처리", userId);
            return true;
        } catch (Exception e) {
            log.error("사용자 ID: {} 로그아웃 중 오류 발생: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    private void saveRefreshToken(String refreshToken, User user, Duration expiredAt) {
        // 1. 만료 시간 로컬 시간으로 설정 (KST)
        LocalDateTime expiryDate = LocalDateTime.now().plus(expiredAt);

        // 2. 기존 토큰이 있는지 조회
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        if (existingToken.isPresent()) {
            RefreshToken tokenEntity = existingToken.get();
            log.info("Before update: {}", tokenEntity.getToken());

            // 기존 엔티티 업데이트
            tokenEntity.update(refreshToken, expiryDate);

            log.info("After update: {}", tokenEntity.getToken());
            refreshTokenRepository.save(tokenEntity);
            return;
        }

        // 3. 없으면 새로운 엔티티 생성
        RefreshToken tokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(expiryDate)
                .build();

        log.info("새로운 Refresh Token 생성: {}", tokenEntity.getToken());

        refreshTokenRepository.save(tokenEntity);
    }
}
