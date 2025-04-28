package inha.gdgoc.domain.user.service;

import static inha.gdgoc.util.EncryptUtil.encrypt;
import static inha.gdgoc.util.EncryptUtil.generateSalt;

import inha.gdgoc.domain.auth.dto.request.FindIdRequest;
import inha.gdgoc.domain.user.dto.request.UserSignupRequest;
import inha.gdgoc.domain.auth.dto.response.FindIdResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
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
