package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.dto.response.GetApplicationResponse;
import inha.gdgoc.domain.study.dto.response.GetAttendeeListResponse;
import inha.gdgoc.domain.study.dto.response.GetAttendeeResponse;
import inha.gdgoc.domain.study.dto.response.PageResponse;
import inha.gdgoc.domain.study.entity.StudyAttendee;
import inha.gdgoc.domain.study.repository.StudyAttendeeRepository;
import inha.gdgoc.global.common.ApiResponse;
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
public class StudyAttendeeService {

    private final StudyAttendeeRepository studyAttendeeRepository;
    private static final Long STUDY_ATTENDEE_PAGE_COUNT = 10L;

    public ApiResponse<GetAttendeeListResponse> getAttendeeList(Long studyId, Optional<Long> _page) {
        Long page = _page.orElse(1L);
        if (page < 1) {
            throw new RuntimeException("page가 1보다 작을 수 없습니다.");
        }

        Long limit = STUDY_ATTENDEE_PAGE_COUNT;
        Long offset = (page - 1) * limit;

        Long study_attendee_count = studyAttendeeRepository.findAllByStudyIdStudyAttendeeCount(studyId);
        List<StudyAttendee> studyAttendee = studyAttendeeRepository.pageAllByStudyId(studyId, limit, offset);
        List<GetAttendeeResponse> attendees = studyAttendee.stream()
                .map(GetAttendeeResponse::from)
                .toList();

        PageResponse meta = new PageResponse(
                page.intValue(),
                study_attendee_count.intValue()
        );

        return ApiResponse.of(new GetAttendeeListResponse(attendees), meta);
    }

    public GetApplicationResponse getApplication(Long studyId, Long attendeeId) {
        StudyAttendee studyAttendee = studyAttendeeRepository.findStudyAttendeeByStudyIdAndUserId(studyId, attendeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디에 지원한 지원자 정보가 없습니다."));

        return GetApplicationResponse.from(studyAttendee);
    }


    public Object createAttendee() {
        return new Object();
    }

    public Object updateAttendee() {
        return new Object();
    }
}
