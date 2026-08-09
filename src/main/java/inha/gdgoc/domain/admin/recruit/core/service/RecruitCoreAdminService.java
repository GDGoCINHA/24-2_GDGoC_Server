package inha.gdgoc.domain.admin.recruit.core.service;

import inha.gdgoc.domain.admin.recruit.core.dto.request.RecruitCoreApplicationAcceptRequest;
import inha.gdgoc.domain.admin.recruit.core.dto.request.RecruitCoreApplicationRejectRequest;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicantDetailResponse;
import inha.gdgoc.domain.admin.recruit.core.dto.response.RecruitCoreApplicationDecisionResponse;
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.recruit.core.repository.RecruitCoreApplicationRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruitCoreAdminService {

    private final RecruitCoreApplicationRepository repository;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public Page<RecruitCoreApplication> searchApplications(
        String session,
        RecruitCoreResultStatus status,
        TeamType team,
        Pageable pageable
    ) {
        Specification<RecruitCoreApplication> spec = Specification.where(bySession(session));
        if (status != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("resultStatus"), status));
        }
        if (team != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("team"), team.name()));
        }
        return repository.findAll(spec, pageable);
    }

    // 저장된 fileUrls 는 S3 키다(웹이 presigned 업로드 후 key 를 보낸다).
    // 키를 그대로 내려주면 대시보드의 링크가 상대 경로가 되어 첨부 파일이 열리지 않는다.
    @Transactional(readOnly = true)
    public RecruitCoreApplicantDetailResponse getApplicationDetail(Long applicationId) {
        RecruitCoreApplication application = getApplication(applicationId);
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

    @Transactional
    public RecruitCoreApplicationDecisionResponse accept(
        Long applicationId,
        Long reviewerId,
        RecruitCoreApplicationAcceptRequest request
    ) {
        RecruitCoreApplication application = getApplication(applicationId);
        ensureDecidable(application);
        Instant now = Instant.now();
        application.accept(reviewerId, request.resultNote(), now);

        User applicant = application.getUser();
        if (!UserRole.hasAtLeast(applicant.getUserRole(), UserRole.CORE)) {
            applicant.changeRole(UserRole.CORE);
        }
        TeamType applicantTeam = applicant.getTeam();
        TeamType applicationTeam = teamTypeOf(application.getTeam());
        if (applicationTeam != null && (Boolean.TRUE.equals(request.overwriteTeamIfExists()) || applicantTeam == null)) {
            applicant.changeTeam(applicationTeam);
            applicantTeam = applicationTeam;
        }

        return RecruitCoreApplicationDecisionResponse.accepted(
            application,
            applicant.getUserRole(),
            applicantTeam
        );
    }

    @Transactional
    public RecruitCoreApplicationDecisionResponse reject(
        Long applicationId,
        Long reviewerId,
        RecruitCoreApplicationRejectRequest request
    ) {
        RecruitCoreApplication application = getApplication(applicationId);
        ensureDecidable(application);
        Instant now = Instant.now();
        application.reject(reviewerId, request.resultNote(), now);
        return RecruitCoreApplicationDecisionResponse.rejected(application);
    }

    private Specification<RecruitCoreApplication> bySession(String session) {
        return (root, query, builder) -> builder.equal(root.get("session"), Objects.requireNonNull(session));
    }

    private RecruitCoreApplication getApplication(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
    }

    private void ensureDecidable(RecruitCoreApplication application) {
        if (application.getResultStatus() == RecruitCoreResultStatus.ACCEPTED
            || application.getResultStatus() == RecruitCoreResultStatus.REJECTED) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "이미 처리된 지원서입니다.");
        }
    }

    private TeamType teamTypeOf(String team) {
        try {
            return TeamType.from(team);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
