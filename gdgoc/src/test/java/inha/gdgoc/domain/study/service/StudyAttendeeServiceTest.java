package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.dto.AttendeeUpdateDto;
import inha.gdgoc.domain.study.dto.StudyAttendeeListWithMetaDto;
import inha.gdgoc.domain.study.dto.request.AttendeeCreateRequest;
import inha.gdgoc.domain.study.dto.request.AttendeeUpdateRequest;
import inha.gdgoc.domain.study.dto.response.GetStudyAttendeeResponse;
import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.entity.StudyAttendee;
import inha.gdgoc.domain.study.enums.AttendeeStatus;
import inha.gdgoc.domain.study.enums.CreaterType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.study.repository.StudyAttendeeRepository;
import inha.gdgoc.domain.study.repository.StudyRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class StudyAttendeeServiceTest {

    @Autowired
    private StudyAttendeeService studyAttendeeService;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyAttendeeRepository studyAttendeeRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = createUser(UserRole.GUEST);
        userRepository.save(user);
    }

    @DisplayName("스터디 참석자 목록을 페이징하여 조회한다.")
    @Test
    void getAttendeeListPaging() {
        // given
        Study study = createStudy("페이징 참석자 테스트", user);
        studyRepository.save(study);

        for (int i = 0; i < 15; i++) {
            User attendeeUser = createUser(UserRole.GUEST);
            userRepository.save(attendeeUser);

            StudyAttendee attendee = StudyAttendee.builder()
                    .study(study)
                    .user(attendeeUser)
                    .status(AttendeeStatus.APPROVED)
                    .introduce("소개 " + i)
                    .activityTime("시간 " + i)
                    .build();

            studyAttendeeRepository.save(attendee);
        }

        // when
        StudyAttendeeListWithMetaDto pageOneResult = studyAttendeeService.getStudyAttendeeList(
                study.getId(),
                Optional.of(1L)
        );

        StudyAttendeeListWithMetaDto pageTwoResult = studyAttendeeService.getStudyAttendeeList(
                study.getId(),
                Optional.of(2L)
        );

        // then
        assertThat(pageOneResult).isNotNull();
        assertThat(pageOneResult.getAttendees()).hasSize(10);
        assertThat(pageOneResult.getPage()).isEqualTo(1);
        assertThat(pageOneResult.getPageCount()).isGreaterThanOrEqualTo(15);

        assertThat(pageTwoResult).isNotNull();
        assertThat(pageTwoResult.getAttendees()).hasSize(5);
        assertThat(pageTwoResult.getPage()).isEqualTo(2);
        assertThat(pageTwoResult.getPageCount()).isGreaterThanOrEqualTo(15);
    }

    @DisplayName("스터디 지원자의 상세 정보를 조회한다.")
    @Test
    void getStudyAttendeeDetail() {
        // given
        Study study = createStudy("상세 정보 테스트 스터디", user);
        studyRepository.save(study);

        String findName = "테스트";
        String findPhoneNumber = "010-1234-5678";
        String findMajor = "컴퓨터공학과";
        String findStudentId = "12212444";

        String findIntroduce = "저는 사실 엄청 멋있는 사람입니다!";
        String findActivityTime = "수요일만 아니면 다 5시 이후로 가능!";

        User attendeeUser = User.builder()
                .name(findName)
                .phoneNumber(findPhoneNumber)
                .major(findMajor)
                .studentId(findStudentId)
                .email("email@example.com")
                .password("pass")
                .salt(new byte[16])
                .userRole(UserRole.GUEST)
                .build();
        userRepository.save(attendeeUser);

        StudyAttendee attendee = StudyAttendee.builder()
                .study(study)
                .user(attendeeUser)
                .status(AttendeeStatus.REQUESTED)
                .introduce(findIntroduce)
                .activityTime(findActivityTime)
                .build();
        studyAttendeeRepository.save(attendee);

        // when
        GetStudyAttendeeResponse response = studyAttendeeService.getStudyAttendee(study.getId(), attendeeUser.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(findName);
        assertThat(response.getPhone()).isEqualTo(findPhoneNumber);
        assertThat(response.getMajor()).isEqualTo(findMajor);
        assertThat(response.getStudentId()).isEqualTo(findStudentId);
        assertThat(response.getIntroduce()).isEqualTo(findIntroduce);
        assertThat(response.getActivityTime()).isEqualTo(findActivityTime);
    }


    @DisplayName("스터디에 정상적으로 지원자를 등록한다.")
    @Test
    void createAttendee() {
        // given
        Study study = createStudy("정상 지원 스터디", user);
        studyRepository.save(study);

        User attendeeUser = createUser(UserRole.MEMBER);
        userRepository.save(attendeeUser);

        String findIntroduce = "저는 열정 가득한 사람입니다.";
        String findActivityTime = "주말 오후";

        AttendeeCreateRequest request = AttendeeCreateRequest.builder()
                .introduce(findIntroduce)
                .activityTime(findActivityTime)
                .build();

        // when
        GetStudyAttendeeResponse response = studyAttendeeService.createAttendee(attendeeUser.getId(), study.getId(), request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(attendeeUser.getName());
        assertThat(response.getIntroduce()).isEqualTo(findIntroduce);
        assertThat(response.getActivityTime()).isEqualTo(findActivityTime);
    }


    @DisplayName("GUEST 유저는 스터디에 지원할 수 없다.")
    @Test
    void createAttendee_guestUserForbidden() {
        // given
        Study study = createStudy("게스트 예외 스터디", user);
        studyRepository.save(study);

        User guestUser = createUser(UserRole.GUEST);
        userRepository.save(guestUser);

        AttendeeCreateRequest request = AttendeeCreateRequest.builder()
                .introduce("참여하고 싶어요.")
                .activityTime("평일 오후")
                .build();

        // when & then
        assertThatThrownBy(() -> studyAttendeeService.createAttendee(guestUser.getId(), study.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용 권한이 없는 유저입니다.");
    }

    @DisplayName("스터디 참석자들의 상태를 일괄 수정한다.")
    @Test
    void updateAttendeeStatusBulk() {
        // given
        Study study = createStudy("상태 수정 테스트용 스터디", user);
        studyRepository.save(study);

        User user1 = createUser(UserRole.GUEST);
        User user2 = createUser(UserRole.GUEST);
        userRepository.saveAll(List.of(user1, user2));

        StudyAttendee attendee1 = StudyAttendee.builder()
                .study(study)
                .user(user1)
                .status(AttendeeStatus.REQUESTED)
                .introduce("참석자1")
                .activityTime("월요일")
                .build();

        StudyAttendee attendee2 = StudyAttendee.builder()
                .study(study)
                .user(user2)
                .status(AttendeeStatus.REQUESTED)
                .introduce("참석자2")
                .activityTime("화요일")
                .build();

        studyAttendeeRepository.saveAll(List.of(attendee1, attendee2));

        // when
        AttendeeStatus findStatus_1 = AttendeeStatus.APPROVED;
        AttendeeStatus findStatus_2 = AttendeeStatus.REJECTED;

        AttendeeUpdateRequest updateRequest = AttendeeUpdateRequest.builder()
                .attendees(List.of(
                        AttendeeUpdateDto.builder()
                                .attendeeId(attendee1.getId())
                                .status(findStatus_1)
                                .build(),
                        AttendeeUpdateDto.builder()
                                .attendeeId(attendee2.getId())
                                .status(findStatus_2)
                                .build()
                ))
                .build();

        studyAttendeeService.updateAttendee(study.getId(), updateRequest);

        // then
        StudyAttendee updated1 = studyAttendeeRepository.findById(attendee1.getId()).orElseThrow();
        StudyAttendee updated2 = studyAttendeeRepository.findById(attendee2.getId()).orElseThrow();

        assertThat(updated1.getStatus()).isEqualTo(findStatus_1);
        assertThat(updated2.getStatus()).isEqualTo(findStatus_2);
    }


    private User createUser(
            UserRole userRole
    ) {
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        return User.builder()
                .name("name")
                .major("major")
                .studentId("studentId")
                .phoneNumber("phoneNumber")
                .email("email")
                .password("hashedPassword")
                .salt(salt)
                .userRole(userRole)
                .studies(new ArrayList<>())
                .studyAttendees(new ArrayList<>())
                .build();
    }

    private Study createStudy(
            String title,
            User user
    ) {
        return Study.builder()
                .title(title)
                .simpleIntroduce("간단한 소개")
                .activityIntroduce("활동 소개")
                .imagePath("test url")
                .createrType(CreaterType.PERSONAL)
                .status(StudyStatus.RECRUITED)
                .expectedTime("매일매일")
                .expectedPlace("인하대정문")
                .recruitStartDate(LocalDateTime.now())
                .recruitEndDate(LocalDateTime.now())
                .activityStartDate(LocalDateTime.now())
                .activityEndDate(LocalDateTime.now())
                .user(user)
                .build();
    }
}