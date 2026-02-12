package inha.gdgoc.domain.admin.recruit.core.service;

import inha.gdgoc.domain.admin.recruit.core.dto.request.RecruitCoreApplicationAcceptRequest;
import inha.gdgoc.domain.admin.recruit.core.dto.request.RecruitCoreApplicationRejectRequest;
import inha.gdgoc.domain.admin.recruit.core.dto.response.RecruitCoreApplicationDecisionResponse;
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.recruit.core.repository.RecruitCoreApplicationRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.time.Instant;
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
        if (team == null) {
            return null;
        }
        try {
            return TeamType.valueOf(team);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
