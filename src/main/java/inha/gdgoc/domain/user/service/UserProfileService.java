package inha.gdgoc.domain.user.service;

import static inha.gdgoc.domain.user.exception.UserErrorCode.INVALID_MAJOR;
import static inha.gdgoc.domain.user.exception.UserErrorCode.INVALID_PHONE_NUMBER;
import static inha.gdgoc.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.dto.request.UpdateUserProfileRequest;
import inha.gdgoc.domain.user.dto.response.UserProfileResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.exception.UserException;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.util.MajorNormalizer;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^01[0-9]\\d{7,8}$");

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final MajorNormalizer majorNormalizer;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    public UserProfileResponse updateMyProfile(Long userId, UpdateUserProfileRequest request) {
        User user = findUser(userId);

        String major = majorNormalizer.normalize(request.major());
        if (!majorNormalizer.isKnownCode(major)) {
            throw new UserException(INVALID_MAJOR);
        }

        String phoneNumber = request.phoneNumber().trim();
        if (!PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
            throw new UserException(INVALID_PHONE_NUMBER);
        }

        user.updateProfile(request.name().trim(), major, phoneNumber);
        return UserProfileResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
    }
}
