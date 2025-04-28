package inha.gdgoc.domain.user.service;

import inha.gdgoc.domain.auth.dto.request.FindIdRequest;
import inha.gdgoc.domain.user.dto.request.UserSignupRequest;
import inha.gdgoc.domain.auth.dto.response.FindIdResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<Long> getAllUserIds() {
        return userRepository.findAllUsers().stream()
                .map((User::getId))
                .toList();
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

    public void saveUser(UserSignupRequest userSignupRequest) {
        byte[] salt = generateSalt();
        String hashedPassword = encrypt(userSignupRequest.getPassword(), salt);

        User user = userSignupRequest.toEntity(hashedPassword, salt);
        userRepository.save(user);
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
}
