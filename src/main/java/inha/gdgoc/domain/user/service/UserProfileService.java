package inha.gdgoc.domain.user.service;

import static inha.gdgoc.domain.user.exception.UserErrorCode.INVALID_IMAGE_FILE;
import static inha.gdgoc.domain.user.exception.UserErrorCode.INVALID_MAJOR;
import static inha.gdgoc.domain.user.exception.UserErrorCode.INVALID_PHONE_NUMBER;
import static inha.gdgoc.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.dto.request.UpdateUserProfileRequest;
import inha.gdgoc.domain.user.dto.response.UserImageResponse;
import inha.gdgoc.domain.user.dto.response.UserProfileResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.exception.UserException;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.util.MajorNormalizer;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^01[0-9]\\d{7,8}$");
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp");
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

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

    public UserImageResponse updateMyImage(Long userId, MultipartFile file) {
        User user = findUser(userId);

        if (file == null || file.isEmpty()) {
            throw new UserException(INVALID_IMAGE_FILE);
        }
        if (file.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new UserException(INVALID_IMAGE_FILE);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new UserException(INVALID_IMAGE_FILE);
        }

        String url;
        try {
            String key = s3Service.upload(userId, S3KeyType.profile, file);
            url = s3Service.getS3FileUrl(key);
        } catch (IOException e) {
            log.warn("프로필 이미지 S3 업로드 실패 - userId: {}", userId, e);
            throw new UserException(INVALID_IMAGE_FILE);
        }

        user.updateImage(url);
        return new UserImageResponse(url);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
    }
}
