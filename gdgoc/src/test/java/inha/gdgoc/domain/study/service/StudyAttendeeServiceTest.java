package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.dto.StudyAttendeeListWithMetaDto;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
        user = createUser();
        userRepository.save(user);
    }

    @DisplayName("스터디 참석자 목록을 페이징하여 조회한다.")
    @Test
    void getAttendeeListPaging() {
        // given
        Study study = createStudy("페이징 참석자 테스트", user);
        studyRepository.save(study);

        for (int i = 0; i < 15; i++) {
            User attendeeUser = createUser();
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

    private User createUser() {
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
                .userRole(UserRole.GUEST)
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