package inha.gdgoc.domain.auth.service;

import inha.gdgoc.config.jwt.TokenProvider;
import inha.gdgoc.domain.auth.dto.request.FindIdRequest;
import inha.gdgoc.domain.auth.dto.request.UserSignupRequest;
import inha.gdgoc.domain.auth.dto.response.FindIdResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    // TODO email 중복 조회

    public void saveUser(UserSignupRequest userSignupRequest) {
        byte[] salt = generateSalt();
        String hashedPassword = encrypt(userSignupRequest.getPassword(), salt);

        User user = userSignupRequest.toEntity(hashedPassword, salt);
        userRepository.save(user);
    }

    public FindIdResponse findId(FindIdRequest findIdRequest) {
        Optional<User> user = userRepository.findByNameAndMajorAndPhoneNumber(
                findIdRequest.getName(),
                findIdRequest.getMajor(),
                findIdRequest.getPhoneNumber()
        );

        if (user.isEmpty()) {
            throw new IllegalArgumentException("해당 정보를 가진 사용자를 찾을 수 없습니다.");
        }

        String email = user.get().getEmail();
        String maskedEmail = maskEmail(email);

        return new FindIdResponse(maskedEmail);
    }

    private String encrypt(String oldPassword, byte[] salt) {
        return generateHashedValue(oldPassword, salt);
    }

    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    private String generateHashedValue(String oldPassword, byte[] salt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(salt, "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hashedBytes = mac.doFinal(oldPassword.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while hashing password", e);
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 5) {
            // 너무 짧은 이메일은 앞 글자 1개만 보이게 처리
            return email.charAt(0) + "*****" + email.substring(atIndex);
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        int startLen = 2;
        int endLen = 2;
        int maskLen = Math.max(1, localPart.length() - startLen - endLen); // 최소 1개는 마스킹

        return localPart.substring(0, startLen)
                + "*".repeat(maskLen)
                + localPart.substring(localPart.length() - endLen)
                + domainPart;
    }

    public Long getAuthenticationUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof TokenProvider.CustomUserDetails user) {
            return user.getUserId();
        }
        throw new IllegalArgumentException("user Id is null");
    }
}
