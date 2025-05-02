package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.study.dto.request.AttendeeCreateRequest;
import inha.gdgoc.domain.study.dto.response.GetApplicationResponse;
import inha.gdgoc.domain.study.dto.response.GetAttendeeListResponse;
import inha.gdgoc.domain.study.dto.response.GetAttendeeResponse;
import inha.gdgoc.domain.study.dto.response.PageResponse;
import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.entity.StudyAttendee;
import inha.gdgoc.domain.study.enums.AttendeeStatus;
import inha.gdgoc.domain.study.repository.StudyAttendeeRepository;
import inha.gdgoc.domain.study.repository.StudyRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.common.ApiResponse;
import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudyAttendeeService {

    private final StudyAttendeeRepository studyAttendeeRepository;
    private final DefaultAuthenticationEventPublisher authenticationEventPublisher;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;

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

    public GetApplicationResponse getApplication(Long studyId, Long attendeeId) {
        StudyAttendee studyAttendee = studyAttendeeRepository.findStudyAttendeeByStudyIdAndUserId(studyId, attendeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디에 지원한 지원자 정보가 없습니다."));

        return GetApplicationResponse.from(studyAttendee);
    }

    public void createAttendee(
            Authentication authentication,
            Long studyId,
            AttendeeCreateRequest attendeeCreateRequest
    ) {
        Long userId = authService.getAuthenticationUserId(authentication);
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User foundUser = user.get();
        if (foundUser.getUserRole().equals(UserRole.GUEST)) {
            throw new IllegalArgumentException("사용 권한이 없는 유저입니다.");
        }

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디가 존재하지 않습니다."));

        studyAttendeeRepository.save(
                StudyAttendee.create(
                        AttendeeStatus.REQUESTED,
                        attendeeCreateRequest.introduce(),
                        attendeeCreateRequest.activityTime(),
                        study,
                        foundUser));
    }

    public Object updateAttendee() {
        return new Object();
    }
}
