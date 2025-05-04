package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.dto.StudyDto;
import inha.gdgoc.domain.study.dto.request.StudyCreateRequest;
import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.enums.CreaterType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.study.repository.StudyRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.exception.NotFoundException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
@Transactional
class StudyServiceTest {

    @Autowired
    private StudyService studyService;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = createUser();
        userRepository.save(user);
    }


    @DisplayName("해당 스터디 정보를 id로 조회한다.")
    @Test
    void getStudyById() {
        // given
        String findTitle = "테스트제목";
        Study findStudy = createStudy(findTitle, user);
        studyRepository.save(findStudy);

        // when
        StudyDto resultStudy = studyService.getStudyById(findStudy.getId());

        // then
        assertThat(resultStudy).isNotNull();
        assertThat(resultStudy.getCreatorId()).isEqualTo(user.getId());
        assertThat(resultStudy.getTitle()).isEqualTo(findTitle);
    }

    @DisplayName("해당 스터디 id가 없다면 에러가 발생한다.")
    @Test
    void getStudyByIdNotFound() {
        // then
        assertThatThrownBy(() -> {
            studyService.getStudyById(99999L);
        }).isInstanceOf(NotFoundException.class);
    }

    @DisplayName("스터디를 생성한다.")
    @Test
    void createStudy() {
        // given
        String findTitle = "스터디 제목";
        StudyCreateRequest request = StudyCreateRequest.builder()
                .title(findTitle)
                .simpleIntroduce("간단한 소개")
                .activityIntroduce("활동 소개")
                .creatorType(CreaterType.PERSONAL)
                .expectedTime("오후 2시")
                .expectedPlace("인하대학교 도서관")
                .recruitStartDate(LocalDateTime.of(2025, 5, 10, 12, 0))
                .recruitEndDate(LocalDateTime.of(2025, 5, 15, 18, 0))
                .activityStartDate(LocalDateTime.of(2025, 5, 20, 14, 0))
                .activityEndDate(LocalDateTime.of(2025, 6, 20, 16, 0))
                .build();

        // when
        StudyDto result = studyService.createStudy(user.getId(), request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCreatorId()).isEqualTo(user.getId());
        assertThat(result.getTitle()).isEqualTo(findTitle);

        Study saved = studyRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getTitle()).isEqualTo(findTitle);
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