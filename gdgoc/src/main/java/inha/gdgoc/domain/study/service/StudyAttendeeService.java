package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.dto.response.GetAttendeeListResponse;
import inha.gdgoc.domain.study.dto.response.GetAttendeeResponse;
import inha.gdgoc.domain.study.dto.response.PageResponse;
import inha.gdgoc.domain.study.entity.StudyAttendee;
import inha.gdgoc.domain.study.repository.StudyAttendeeRepository;
import inha.gdgoc.global.common.ApiResponse;
import java.awt.print.Pageable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudyAttendeeService {

    private final StudyAttendeeRepository studyAttendeeRepository;

    public ApiResponse<GetAttendeeListResponse> getAttendeeList(Long studyId, Pageable pageable) {
        Page<StudyAttendee> page = studyAttendeeRepository.findAllByStudyId(studyId, pageable);

        List<GetAttendeeResponse> attendees = page.getContent().stream()
                .map(GetAttendeeResponse::from)
                .toList();

        PageResponse meta = new PageResponse(
                page.getNumber(),
                page.getTotalPages()
        );

        return ApiResponse.of(new GetAttendeeListResponse(attendees), meta);
    }


    public Object createAttendee() {
        return new Object();
    }

    public Object updateAttendee() {
        return new Object();
    }
}
