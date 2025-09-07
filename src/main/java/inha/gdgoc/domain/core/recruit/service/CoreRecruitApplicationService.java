package inha.gdgoc.domain.core.recruit.service;

import inha.gdgoc.domain.core.recruit.dto.request.CoreRecruitApplicationRequest;
import inha.gdgoc.domain.core.recruit.dto.response.CoreRecruitApplicantDetailResponse;
import inha.gdgoc.domain.core.recruit.dto.response.CoreRecruitApplicantSummaryResponse;
import inha.gdgoc.domain.core.recruit.entity.CoreRecruitApplication;
import inha.gdgoc.domain.core.recruit.repository.CoreRecruitApplicationRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoreRecruitApplicationService {

    private final CoreRecruitApplicationRepository repository;

    @Transactional
    public Long create(CoreRecruitApplicationRequest request) {
        CoreRecruitApplication entity = CoreRecruitApplication.builder()
            .name(request.getName())
            .studentId(request.getStudentId())
            .phone(request.getPhone())
            .major(request.getMajor())
            .email(request.getEmail())
            .team(request.getTeam())
            .motivation(request.getMotivation())
            .wish(request.getWish())
            .strengths(request.getStrengths())
            .pledge(request.getPledge())
            .fileUrls(request.getFileUrls())
            .build();

        return repository.save(entity).getId();
    }

    @Transactional(readOnly = true)
    public Page<CoreRecruitApplication> findApplicantsPage(String question, Pageable pageable) {
        if (question == null || question.isBlank()) {
            return repository.findAll(pageable);
        }
        return repository.findByNameContainingIgnoreCase(question, pageable);
    }

    @Transactional(readOnly = true)
    public CoreRecruitApplicantDetailResponse getApplicantDetail(Long id) {
        CoreRecruitApplication app = repository.findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
        return CoreRecruitApplicantDetailResponse.from(app);
    }
}


