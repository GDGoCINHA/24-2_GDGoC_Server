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
import inha.gdgoc.domain.recruit.core.repository.RecruitCoreApplicationRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruitCoreApplicationService {

    private final RecruitCoreApplicationRepository repository;
    private final UserRepository userRepository;
    private final RecruitCoreSessionResolver recruitCoreSessionResolver;

    @Transactional(readOnly = true)
    public RecruitCoreApplicantDetailResponse getApplicantDetail(Long id) {
        RecruitCoreApplication app = getApplication(id);
        return RecruitCoreApplicantDetailResponse.from(app);
    }

    @Transactional(readOnly = true)
    public RecruitCoreEligibilityResponse checkEligibility(Long userId) {
        String session = recruitCoreSessionResolver.currentSession();
        return repository.findByUser_IdAndSession(userId, session)
            .map(app -> RecruitCoreEligibilityResponse.ineligible(session, "ALREADY_APPLIED", app.getId()))
            .orElseGet(() -> RecruitCoreEligibilityResponse.eligible(session));
    }

    @Transactional(readOnly = true)
    public RecruitCorePrefillResponse prefill(Long userId) {
        String session = recruitCoreSessionResolver.currentSession();
        repository.findByUser_IdAndSession(userId, session)
            .ifPresent(existing -> {
                throw new RecruitCoreAlreadyAppliedException(session, existing.getId());
            });

        User user = getUser(userId);
        return RecruitCorePrefillResponse.from(user);
    }

    @Transactional
    public RecruitCoreApplicationCreateResponse submit(Long userId, RecruitCoreApplicationCreateRequest request) {
        String session = recruitCoreSessionResolver.currentSession();
        repository.findByUser_IdAndSession(userId, session)
            .ifPresent(existing -> {
                throw new RecruitCoreAlreadyAppliedException(session, existing.getId());
            });

        User user = getUser(userId);
        List<String> fileUrls = request.fileUrls() == null
            ? List.of()
            : List.copyOf(request.fileUrls());
        RecruitCoreApplication application = RecruitCoreApplication.builder()
            .user(user)
            .session(session)
            .name(request.snapshot().name())
            .studentId(request.snapshot().studentId())
            .phone(request.snapshot().phone())
            .major(request.snapshot().major())
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
        RecruitCoreApplication application = repository.findByUser_IdAndSession(userId, session)
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
        return RecruitCoreApplicantDetailResponse.from(application);
    }

    private RecruitCoreApplication getApplication(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
    }

}
