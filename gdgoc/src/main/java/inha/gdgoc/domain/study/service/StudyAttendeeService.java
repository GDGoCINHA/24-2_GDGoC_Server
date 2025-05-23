package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.dto.AttendeeUpdateDto;
import inha.gdgoc.domain.study.dto.StudyAttendeeDto;
import inha.gdgoc.domain.study.dto.StudyAttendeeListWithMetaDto;
import inha.gdgoc.domain.study.dto.StudyAttendeeResultDto;
import inha.gdgoc.domain.study.dto.request.AttendeeCreateRequest;
import inha.gdgoc.domain.study.dto.request.AttendeeUpdateRequest;
import inha.gdgoc.domain.study.dto.response.GetStudyAttendeeResponse;
import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.entity.StudyAttendee;
import inha.gdgoc.domain.study.enums.AttendeeStatus;
import inha.gdgoc.domain.study.repository.StudyAttendeeRepository;
import inha.gdgoc.domain.study.repository.StudyRepository;
import inha.gdgoc.domain.study.validator.CreateStudyAttendeeValidator;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudyAttendeeService {

    private final StudyRepository studyRepository;
    private final StudyAttendeeRepository studyAttendeeRepository;
    private final CreateStudyAttendeeValidator createStudyAttendeeValidator;

    private static final Long STUDY_ATTENDEE_PAGE_COUNT = 10L;
    private final UserRepository userRepository;

    public StudyAttendeeListWithMetaDto getStudyAttendeeList(Long studyId, Optional<Long> _page) {
        Long page = _page.orElse(1L);
        if (page < 1) {
            throw new RuntimeException("page가 1보다 작을 수 없습니다.");
        }

        Long limit = STUDY_ATTENDEE_PAGE_COUNT;
        Long offset = (page - 1) * limit;

        Long StudyAttendeeCount = studyAttendeeRepository.findAllByStudyIdStudyAttendeeCount(studyId);
        List<StudyAttendeeDto> attendees = studyAttendeeRepository.pageAllByStudyId(studyId, limit, offset).stream()
                .map(this::studyAttendeeEntityToDto)
                .toList();

        return StudyAttendeeListWithMetaDto.builder()
                .attendees(attendees)
                .pageCount(StudyAttendeeCount)
                .page(page)
                .build();
    }

    public GetStudyAttendeeResponse getStudyAttendee(Long authenticatedUser, Long studyId, Long attendeeId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 스터디입니다."));
        if (!userRepository.existsById(attendeeId)) {
            throw new RuntimeException("존재하지 않는 유저입니다.");
        }
        if (!study.isCreatedBy(authenticatedUser)) {
            throw new RuntimeException("본인이 만든 스터디의 지원자 정보만 확인할 수 있습니다.");
        }

        StudyAttendee studyAttendee = studyAttendeeRepository.findStudyAttendeeByStudyIdAndUserId(studyId, attendeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디에 지원한 지원자 정보가 없습니다."));
        User stuatAttendeeUser = studyAttendee.getUser();
        return GetStudyAttendeeResponse.builder()
                .name(stuatAttendeeUser.getName())
                .phone(stuatAttendeeUser.getPhoneNumber())
                .major(stuatAttendeeUser.getMajor())
                .studentId(stuatAttendeeUser.getStudentId())
                .introduce(studyAttendee.getIntroduce())
                .activityTime(studyAttendee.getActivityTime())
                .build();
    }

    public List<StudyAttendeeResultDto> getStudyAttendeeResultListByUserId(
            Long userId
    ) {
        List<StudyAttendee> studyAttendeeList = studyAttendeeRepository.findAllByUserId(userId);
        return studyAttendeeList.stream().map(attendee -> StudyAttendeeResultDto.builder()
                        .studyId(attendee.getStudy().getId())
                        .title(attendee.getStudy().getTitle())
                        .recruitEndDate(attendee.getStudy().getRecruitEndDate())
                        .status(attendee.getStatus())
                        .build())
                .toList();
    }

    @Transactional
    public GetStudyAttendeeResponse createAttendee(
            Long userId,
            Long studyId,
            AttendeeCreateRequest attendeeCreateRequest
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디입니다."));
        validateIsApplied(studyId, userId);
        createStudyAttendeeValidator.validateAll(user, study);

        StudyAttendee studyAttendee = StudyAttendee.create(
                AttendeeStatus.REQUESTED,
                attendeeCreateRequest.getIntroduce(),
                attendeeCreateRequest.getActivityTime(),
                study,
                user
        );
        studyAttendeeRepository.save(studyAttendee);

        return GetStudyAttendeeResponse.builder()
                .name(user.getName())
                .phone(user.getPhoneNumber())
                .major(user.getMajor())
                .studentId(user.getStudentId())
                .introduce(studyAttendee.getIntroduce())
                .activityTime(studyAttendee.getActivityTime())
                .build();
    }

    @Transactional
    public void updateAttendee(
            Long userId,
            Long studyId,
            AttendeeUpdateRequest request
    ) {
        List<AttendeeUpdateDto> attendees = request.getAttendees();
        List<Long> attendeeIds = attendees.stream().map(AttendeeUpdateDto::getAttendeeId).toList();
        Study study = studyRepository.findOneWithUserById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디가 존재하지 않습니다."));
        List<StudyAttendee> studyAttendeeList = studyAttendeeRepository.findAllByIdsAndStudyId(attendeeIds, studyId);

        if (!Objects.equals(userId, study.getUser().getId())) {
            throw new IllegalArgumentException("해당 study creator 가 아닙니다. userId: " + userId + " studyId: " + studyId);
        }

        Map<Long, StudyAttendee> studyAttendeeMap = studyAttendeeList.stream()
                .collect(Collectors.toMap(StudyAttendee::getId, Function.identity()));
        attendees.forEach(attendee -> {
            StudyAttendee studyAttendee = studyAttendeeMap.get(attendee.getAttendeeId());
            studyAttendee.setStatus(attendee.getStatus());
        });
        studyAttendeeRepository.saveAll(studyAttendeeList);
    }

    private StudyAttendeeDto studyAttendeeEntityToDto(StudyAttendee studyAttendee) {
        return StudyAttendeeDto.builder()
                .id(studyAttendee.getId())
                .name(studyAttendee.getUser().getName())
                .major(studyAttendee.getUser().getMajor())
                .studentId(studyAttendee.getUser().getStudentId())
                .status(studyAttendee.getStatus())
                .build();
    }

    private void validateIsApplied(Long studyId, Long userId) {
        if (studyAttendeeRepository.existsByStudyIdAndUserId(studyId, userId)) {
            throw new IllegalArgumentException("이미 가입한 스터디입니다.");
        }
    }
}
