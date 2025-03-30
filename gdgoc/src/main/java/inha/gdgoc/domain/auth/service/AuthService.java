package inha.gdgoc.domain.auth.service;

import inha.gdgoc.domain.auth.dto.request.UserSignupRequest;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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


}
