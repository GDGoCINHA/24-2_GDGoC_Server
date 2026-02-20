package inha.gdgoc.domain.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import inha.gdgoc.domain.auth.dto.GoogleUserInfo;
import inha.gdgoc.domain.auth.dto.request.SignupRequest;
import inha.gdgoc.domain.auth.dto.response.AuthUserResponse;
import inha.gdgoc.domain.auth.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.auth.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.auth.dto.response.LoginSuccessResponse;
import inha.gdgoc.domain.auth.dto.response.SignupNeededResponse;
import inha.gdgoc.domain.auth.dto.response.TokenDto;
import inha.gdgoc.domain.auth.entity.AdminCredential;
import inha.gdgoc.domain.auth.repository.AdminCredentialRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.config.jwt.TokenProvider;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.security.AccessGuard;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
  private static final String REFRESH_TOKEN_PREFIX = "RT:";
  private static final String SESSION_VALUE_DELIMITER = "::";

  private final UserRepository userRepository;
  private final AdminCredentialRepository adminCredentialRepository;
  private final TokenProvider tokenProvider;
  private final StringRedisTemplate redisTemplate;
  private final AccessGuard accessGuard;
  private final PasswordEncoder passwordEncoder;

  @Value("${google.client-id}")
  private String googleClientId;

  // 로그인
    @Transactional
    public Object login(String idToken) {
      log.info("로그인 시도 - ID Token 존재 여부: {}", (idToken != null && !idToken.isBlank()));
      //  Google ID Token 검증
      GoogleUserInfo googleUser = verifyGoogleToken(idToken);
      log.info("Google 토큰 검증 성공 - Email: {}, Sub: {}", googleUser.getEmail(), googleUser.getSub());
  
      // 도메인 검증 (인하대 메일만 허용)
      if (!googleUser.getEmail().endsWith("@inha.edu")) {
        log.warn("허용되지 않은 도메인 로그인 시도: {}", googleUser.getEmail());
        throw new IllegalArgumentException("인하대학교(@inha.edu) 계정만 이용 가능합니다.");
      }
  
      //  DB에서 유저 조회 (OAuth Subject 기준)
      User user = userRepository.findByOauthSubject(googleUser.getSub()).orElse(null);
  
      // 신규 유저 -> 회원가입 필요 응답 (202 or 200 with isNewUser=true)
      if (user == null) {
        log.info("신규 유저 감지 - Email: {}", googleUser.getEmail());
        String preferredName =
            hasText(googleUser.getFamilyName()) ? googleUser.getFamilyName() : googleUser.getName();
        return SignupNeededResponse.builder()
            .isNewUser(true)
            .oauthSubject(googleUser.getSub())
            .email(googleUser.getEmail())
            .name(preferredName)
            .picture(googleUser.getPicture())
            .build();
      }
  
      log.info("기존 유저 로그인 - UserID: {}, Email: {}", user.getId(), user.getEmail());
      // 기존 유저 -> 토큰 발급 및 로그인 성공 응답
      TokenDto tokens = generateTokens(user);
      return LoginSuccessResponse.of(tokens, AuthUserResponse.from(user));
    }
  // 회원가입
  @Transactional
  public LoginSuccessResponse signup(SignupRequest request) {
    // 학번 중복 체크
    if (userRepository.existsByStudentId(request.getStudentId())) {
      throw new IllegalArgumentException("이미 존재하는 학번입니다.");
    }

    // 전화번호 정규화 (숫자만 남김)
    String cleanPhone = request.getPhoneNumber().replaceAll("[^0-9]", "");
    if (userRepository.existsByPhoneNumber(cleanPhone)) {
      throw new IllegalArgumentException("이미 존재하는 전화번호입니다.");
    }

    // 유저 엔티티 생성 및 저장
    User newUser =
        User.builder()
            .oauthSubject(request.getOauthSubject()) // 구글 sub
            .email(request.getEmail())
            .name(request.getName())
            .studentId(request.getStudentId())
            .major(request.getMajor())
            .phoneNumber(cleanPhone)
            .image(request.getImage())
            // Role(GUEST), Status(PENDING) 등은 User 엔티티 생성자에서 기본값 처리됨
            .build();

    userRepository.save(newUser);

    // 토큰 발급
    TokenDto tokens = generateTokens(newUser);
    return LoginSuccessResponse.of(tokens, AuthUserResponse.from(newUser));
  }

  @Transactional(readOnly = true)
  public CheckStudentIdResponse isRegisteredStudentId(String studentId) {
    boolean exists = userRepository.existsByStudentId(studentId);
    return new CheckStudentIdResponse(exists);
  }

  @Transactional(readOnly = true)
  public CheckPhoneNumberResponse isRegisteredPhoneNumber(String phoneNumber) {
    String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
    boolean exists = userRepository.existsByPhoneNumber(cleanPhone);
    return new CheckPhoneNumberResponse(exists);
  }

  public boolean hasRequiredAccess(CustomUserDetails me, UserRole role, TeamType requiredTeam) {
    var conditions = new java.util.ArrayList<AccessGuard.AccessCondition>();
    conditions.add(AccessGuard.AccessCondition.atLeast(role));

    if (requiredTeam != null) {
      conditions.add(AccessGuard.AccessCondition.atLeast(UserRole.ORGANIZER));
      conditions.add(AccessGuard.AccessCondition.of(UserRole.GUEST, requiredTeam));
    }

    return accessGuard.check(me, conditions.toArray(AccessGuard.AccessCondition[]::new));
  }

  public RefreshResult refresh(String refreshToken) {
    RefreshSession session = resolveRefreshSession(refreshToken);
    if (session.principalType() == PrincipalType.ADMIN) {
      AdminCredential credential = adminCredentialRepository
              .findById(session.principalId())
              .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자 계정입니다."));
      if (!credential.isEnabled()) {
        throw new IllegalArgumentException("비활성화된 관리자 계정입니다.");
      }
      String accessToken = tokenProvider.createAdminAccessToken(
              credential.getId(),
              credential.getLoginId(),
              session.sessionId()
      );
      return new RefreshResult(accessToken, AuthUserResponse.admin(credential.getLoginId()));
    }

    User user = userRepository
            .findById(session.principalId())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

    String accessToken = tokenProvider.createAccessToken(user, session.sessionId());
    return new RefreshResult(accessToken, AuthUserResponse.from(user));
  }

  // 로그아웃
  public void logout(String refreshToken) {
    // Redis에서 Refresh Token 삭제
    String redisKey = refreshTokenKey(refreshToken);
    redisTemplate.delete(redisKey);
  }

  public Long getAuthenticationUserId(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    if (principal instanceof TokenProvider.CustomUserDetails user) {
      return user.getUserId();
    }
    throw new IllegalArgumentException("User ID not found in authentication");
  }

  // 토큰 발급 및 Redis 저장

  private TokenDto generateTokens(User user) {
    String sessionId = UUID.randomUUID().toString();
    String accessToken = tokenProvider.createAccessToken(user, sessionId);
    String refreshToken = tokenProvider.createRefreshToken();
    storeRefreshSession(refreshToken, new RefreshSession(sessionId, PrincipalType.USER, user.getId()));

    return new TokenDto(accessToken, refreshToken);
  }

  private TokenDto generateAdminTokens(AdminCredential credential) {
    String sessionId = UUID.randomUUID().toString();
    String accessToken = tokenProvider.createAdminAccessToken(
            credential.getId(),
            credential.getLoginId(),
            sessionId
    );
    String refreshToken = tokenProvider.createRefreshToken();
    storeRefreshSession(
            refreshToken,
            new RefreshSession(sessionId, PrincipalType.ADMIN, credential.getId())
    );
    return new TokenDto(accessToken, refreshToken);
  }

  // Google ID Token 검증
  private GoogleUserInfo verifyGoogleToken(String idTokenString) {
    try {
      GoogleIdTokenVerifier verifier =
          new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
              .setAudience(Collections.singletonList(googleClientId))
              .setIssuers(java.util.Arrays.asList("https://accounts.google.com", "accounts.google.com"))
              .build();

      GoogleIdToken idToken = verifier.verify(idTokenString);

      if (idToken != null) {
        GoogleIdToken.Payload payload = idToken.getPayload();
        return buildGoogleUserInfo(payload);
      } else {
        throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
      }
    } catch (GeneralSecurityException | IOException e) {
      log.error("Google Token Verification Failed", e);
      throw new IllegalArgumentException("토큰 검증 실패", e);
    }
  }

  private void storeRefreshSession(String refreshToken, RefreshSession session) {
    redisTemplate
        .opsForValue()
        .set(refreshTokenKey(refreshToken), encodeSessionValue(session), REFRESH_TOKEN_TTL);
  }

  private RefreshSession resolveRefreshSession(String refreshToken) {
    String redisKey = refreshTokenKey(refreshToken);
    String storedValue = redisTemplate.opsForValue().get(redisKey);

    if (storedValue == null) {
      throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
    }

    if (storedValue.contains(SESSION_VALUE_DELIMITER)) {
      return decodeSessionValue(storedValue);
    }

    // 레거시 포맷(oauthSubject만 저장) 호환 처리
    User user =
        userRepository
            .findByOauthSubject(storedValue)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

    RefreshSession upgraded = new RefreshSession(UUID.randomUUID().toString(), PrincipalType.USER, user.getId());
    storeRefreshSession(refreshToken, upgraded);
    return upgraded;
  }

  private RefreshSession decodeSessionValue(String storedValue) {
    String[] parts = storedValue.split(SESSION_VALUE_DELIMITER);
    if (parts.length < 2) {
      throw new IllegalArgumentException("잘못된 세션 정보입니다.");
    }
    try {
      if (parts.length == 2) {
        Long userId = Long.parseLong(parts[1]);
        return new RefreshSession(parts[0], PrincipalType.USER, userId);
      }

      PrincipalType type = PrincipalType.valueOf(parts[1]);
      Long principalId = Long.parseLong(parts[2]);
      return new RefreshSession(parts[0], type, principalId);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("잘못된 세션 정보입니다.", e);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("잘못된 세션 타입 정보입니다.", e);
    }
  }

  private String encodeSessionValue(RefreshSession session) {
    return session.sessionId()
            + SESSION_VALUE_DELIMITER
            + session.principalType().name()
            + SESSION_VALUE_DELIMITER
            + session.principalId();
  }

  private String refreshTokenKey(String refreshToken) {
    return REFRESH_TOKEN_PREFIX + refreshToken;
  }

  private enum PrincipalType {
    USER,
    ADMIN
  }

  private record RefreshSession(String sessionId, PrincipalType principalType, Long principalId) {}

  public record RefreshResult(String accessToken, AuthUserResponse user) {}

  private GoogleUserInfo buildGoogleUserInfo(GoogleIdToken.Payload payload) {
    String fullName = (String) payload.get("name");
    String givenName = (String) payload.get("given_name");
    String familyName = (String) payload.get("family_name");

    NameParts parts = deriveNameParts(fullName);

    String resolvedGiven = hasText(givenName) ? givenName : parts.givenName();
    String resolvedFamily = hasText(familyName) ? familyName : parts.familyName();

    return GoogleUserInfo.builder()
        .sub(payload.getSubject())
        .email(payload.getEmail())
        .name(fullName)
        .givenName(resolvedGiven)
        .familyName(resolvedFamily)
        .picture((String) payload.get("picture"))
        .build();
  }

  private NameParts deriveNameParts(String rawName) {
    if (!hasText(rawName)) {
      return new NameParts("", "");
    }
    String trimmed = rawName.trim();

    if (trimmed.contains(" ")) {
      String[] tokens = trimmed.split("\\s+");
      if (tokens.length == 1) {
        return new NameParts("", tokens[0]);
      }
      String given = tokens[tokens.length - 1];
      String family = String.join(" ", java.util.Arrays.copyOf(tokens, tokens.length - 1)).trim();
      return new NameParts(family, given);
    }

    if (trimmed.length() >= 2) {
      return new NameParts(trimmed.substring(0, 1), trimmed.substring(1));
    }

    return new NameParts(trimmed, "");
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private record NameParts(String familyName, String givenName) {}

  @Transactional(readOnly = true)
  public LoginSuccessResponse adminLogin(String adminId, String password) {
    if (!hasText(adminId) || !hasText(password)) {
      throw new IllegalArgumentException("관리자 아이디/비밀번호를 입력해 주세요.");
    }

    var credential = adminCredentialRepository.findByLoginId(adminId.trim())
            .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다."));

    if (!credential.isEnabled()) {
      throw new IllegalArgumentException("비활성화된 관리자 계정입니다.");
    }

    if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
      throw new IllegalArgumentException("관리자 아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    TokenDto tokens = generateAdminTokens(credential);
    return LoginSuccessResponse.of(tokens, AuthUserResponse.admin(credential.getLoginId()));
  }
}
