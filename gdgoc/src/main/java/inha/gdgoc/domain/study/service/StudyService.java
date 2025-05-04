package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.dto.StudyDto;
import inha.gdgoc.domain.study.dto.StudyListWithMetaDto;
import inha.gdgoc.domain.study.dto.request.StudyCreateRequest;
import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.enums.CreaterType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.study.repository.StudyRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.service.UserService;
import inha.gdgoc.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudyService {

    private final UserService userService;
    private final StudyRepository studyRepository;
    private static final Long STUDY_PAGE_COUNT = 10L;

    public StudyListWithMetaDto getStudyList(
            Optional<Long> _page,
            Optional<StudyStatus> status,
            Optional<CreaterType> creatorType
    ) {
        Long page = _page.orElse(1L);

        if (page < 1) {
            throw new RuntimeException("page가 1보다 작을 수 없습니다.");
        }

        Long limit = STUDY_PAGE_COUNT;
        Long offset = (page - 1) * limit;


        Long count = studyRepository.findAllCountByStatusAndCreatorType(status, creatorType);
        List<Study> studyList = studyRepository.findAllByStatusAndCreatorType(
                status,
                creatorType,
                limit,
                offset
        );

        List<StudyDto> studyDtoList = studyList.stream().map(this::studyEntityToDto).toList();
        return StudyListWithMetaDto.builder()
                .studyList(studyDtoList)
                .page(page)
                .pageCount(count)
                .build();
    }

    public StudyDto getStudyById(Long studyId) {
        Study study = studyRepository.findOneWithUserById(studyId)
                .orElseThrow(() -> new NotFoundException("Study not found with id: " + studyId));
        return studyEntityToDto(study);
    }

    public StudyDto createStudy(
            Long userId,
            StudyCreateRequest body
    ) {
        User user = userService.findUserById(userId);
        Study createdStudy = Study.create(
                body.getTitle(),
                body.getSimpleIntroduce(),
                body.getActivityIntroduce(),
                null,
                body.getCreatorType(),
                StudyStatus.RECRUITING,
                body.getExpectedTime(),
                body.getExpectedPlace(),
                body.getRecruitStartDate(),
                body.getRecruitEndDate(),
                body.getActivityStartDate(),
                body.getActivityEndDate(),
                user
        );
        studyRepository.save(createdStudy);
        return studyEntityToDto(createdStudy);
    }


    private StudyDto studyEntityToDto(Study study) {
        return StudyDto.builder()
                .id(study.getId())
                .title(study.getTitle())
                .creatorId(study.getUser().getId())
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
}
