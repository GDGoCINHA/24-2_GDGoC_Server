package inha.gdgoc.domain.core.recruit.service;

import inha.gdgoc.domain.core.recruit.dto.request.CoreRecruitApplicationRequest;
import inha.gdgoc.domain.core.recruit.entity.CoreRecruitApplication;
import inha.gdgoc.domain.core.recruit.repository.CoreRecruitApplicationRepository;
import lombok.RequiredArgsConstructor;
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
}


