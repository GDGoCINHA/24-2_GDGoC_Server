package inha.gdgoc.domain.user.service;

import static inha.gdgoc.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

import inha.gdgoc.domain.auth.dto.request.FindIdRequest;
import inha.gdgoc.domain.auth.dto.response.FindIdResponse;
import inha.gdgoc.domain.user.dto.request.CheckDuplicatedEmailRequest;
import inha.gdgoc.domain.user.dto.response.CheckDuplicatedEmailResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.exception.UserException;
import inha.gdgoc.domain.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public CheckDuplicatedEmailResponse isExistsByEmail(CheckDuplicatedEmailRequest request) {
        return new CheckDuplicatedEmailResponse(userRepository.existsByEmail(request.email()));
    }

    public User findUserById(Long userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
    }

    public FindIdResponse findId(FindIdRequest findIdRequest) {
        Optional<User> user = userRepository.findByNameAndMajorAndPhoneNumber(
                findIdRequest.getName(),
                findIdRequest.getMajor(),
                findIdRequest.getPhoneNumber()
        );

        if (user.isEmpty()) {
            throw new UserException(USER_NOT_FOUND);
        }

        String email = user.get().getEmail();
        String maskedEmail = maskEmail(email);

        return new FindIdResponse(maskedEmail);
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