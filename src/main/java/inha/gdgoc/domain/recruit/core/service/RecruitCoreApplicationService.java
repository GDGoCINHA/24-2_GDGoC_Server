package inha.gdgoc.domain.recruit.core.service;

import inha.gdgoc.domain.recruit.core.config.RecruitCoreSessionResolver;
import inha.gdgoc.domain.recruit.core.dto.request.RecruitCoreApplicationCreateRequest;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicantDetailResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicationCreateResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreEligibilityResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreMyApplicationResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCorePrefillResponse;
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreAlreadyAppliedException;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreApplicationNotFoundException;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreClosedException;
import inha.gdgoc.domain.recruit.core.repository.RecruitCoreApplicationRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import inha.gdgoc.global.util.MajorNormalizer;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruitCoreApplicationService {

    private static final Instant RECRUITMENT_DEADLINE = Instant.parse("2026-03-14T14:59:59Z");

    private final RecruitCoreApplicationRepository repository;
    private final UserRepository userRepository;
    private final RecruitCoreSessionResolver recruitCoreSessionResolver;
    private final MajorNormalizer majorNormalizer;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public RecruitCoreApplicantDetailResponse getApplicantDetail(Long id) {
        RecruitCoreApplication app = getApplication(id);
        return RecruitCoreApplicantDetailResponse.from(app, toS3FileUrls(app.getFileUrls()));
    }

    @Transactional(readOnly = true)
    public RecruitCoreEligibilityResponse checkEligibility(Long userId) {
        validateRecruitmentOpen();
        String session = recruitCoreSessionResolver.currentSession();
        return repository.findByUserIdAndSession(userId, session)
            .map(app -> RecruitCoreEligibilityResponse.ineligible(session, "ALREADY_APPLIED", app.getId()))
            .orElseGet(() -> RecruitCoreEligibilityResponse.eligible(session));
    }

    @Transactional(readOnly = true)
    public RecruitCorePrefillResponse prefill(Long userId) {
        validateRecruitmentOpen();
        String session = recruitCoreSessionResolver.currentSession();
        repository.findByUserIdAndSession(userId, session)
            .ifPresent(existing -> {
                throw new RecruitCoreAlreadyAppliedException(session, existing.getId());
            });

        User user = getUser(userId);
        return RecruitCorePrefillResponse.from(user);
    }

    @Transactional
    public RecruitCoreApplicationCreateResponse submit(Long userId, RecruitCoreApplicationCreateRequest request) {
        validateRecruitmentOpen();
        String session = recruitCoreSessionResolver.currentSession();
        repository.findByUserIdAndSession(userId, session)
            .ifPresent(existing -> {
                throw new RecruitCoreAlreadyAppliedException(session, existing.getId());
            });

        User user = getUser(userId);
        List<String> fileUrls = request.fileUrls() == null
            ? List.of()
            : List.copyOf(request.fileUrls());
        String cleanPhone = request.snapshot().phone().replaceAll("[^0-9]", "");
        RecruitCoreApplication application = RecruitCoreApplication.builder()
            .user(user)
            .session(session)
            .name(request.snapshot().name())
            .studentId(request.snapshot().studentId())
            .phone(cleanPhone)
            .major(majorNormalizer.normalize(request.snapshot().major()))
            .email(request.snapshot().email())
            .team(request.team())
            .motivation(request.motivation())
            .wish(request.wish())
            .strengths(request.strengths())
            .pledge(request.pledge())
            .fileUrls(fileUrls)
            .resultStatus(RecruitCoreResultStatus.SUBMITTED)
            .build();

        RecruitCoreApplication saved = repository.save(application);
        return RecruitCoreApplicationCreateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public RecruitCoreMyApplicationResponse getMyApplication(Long userId) {
        String session = recruitCoreSessionResolver.currentSession();
        RecruitCoreApplication application = repository.findByUserIdAndSession(userId, session)
            .orElseThrow(RecruitCoreApplicationNotFoundException::new);
        return RecruitCoreMyApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public RecruitCoreApplicantDetailResponse getApplicantDetailForViewer(
        Long applicationId,
        Long viewerId,
        UserRole viewerRole
    ) {
        RecruitCoreApplication application = repository.findById(applicationId)
            .orElseThrow(RecruitCoreApplicationNotFoundException::new);
        boolean privileged = UserRole.hasAtLeast(viewerRole, UserRole.LEAD);
        if (!privileged && !application.isOwnedBy(viewerId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
        }
        return RecruitCoreApplicantDetailResponse.from(application, toS3FileUrls(application.getFileUrls()));
    }

    private List<String> toS3FileUrls(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return List.of();
        }
        return fileKeys.stream()
            .map(this::toS3FileUrl)
            .toList();
    }

    private String toS3FileUrl(String fileKey) {
        if (fileKey.startsWith("http://") || fileKey.startsWith("https://")) {
            return fileKey;
        }
        return s3Service.getS3FileUrl(fileKey);
    }

    private RecruitCoreApplication getApplication(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateRecruitmentOpen() {
        if (Instant.now().isAfter(RECRUITMENT_DEADLINE)) {
            throw new RecruitCoreClosedException(RECRUITMENT_DEADLINE);
        }
    }

}
