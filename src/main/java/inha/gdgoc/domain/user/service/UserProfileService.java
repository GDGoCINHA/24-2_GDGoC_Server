package inha.gdgoc.domain.user.service;

import static inha.gdgoc.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.dto.response.UserProfileResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.exception.UserException;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.util.MajorNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final MajorNormalizer majorNormalizer;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
    }
}
