package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.dto.StudyDto;
import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.repository.StudyRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;

    public Object getStudyList() {
        return new Object();
    }

    public StudyDto getStudyById(Long studyId) {
        Study study = studyRepository.findOneWithUserById(studyId)
                .orElseThrow(() -> new NotFoundException("Study not found with id: " + studyId));

        User creator = study.getUser();
        return StudyDto.builder()
                .id(study.getId())
                .title(study.getTitle())
                .creatorId(creator.getId())
                .createrType(study.getCreaterType())
                .simpleIntroduce(study.getSimpleIntroduce())
                .activityIntroduce(study.getActivityIntroduce())
                .status(study.getStatus())
                .recruitStartDate(study.getRecruitStartDate())
                .recruitEndDate(study.getRecruitEndDate())
                .activityStartDate(study.getActivityStartDate())
                .activityEndDate(study.getActivityEndDate())
                .expectedTime(study.getExpectedTime())
                .expectedPlace(study.getExpectedPlace())
                .imagePath(study.getImagePath())
                .build();
    }

    public Object createStudy() {
        return new Object();
    }
}
