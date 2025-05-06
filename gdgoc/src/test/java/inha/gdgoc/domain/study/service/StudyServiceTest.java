package inha.gdgoc.domain.study.service;

import inha.gdgoc.domain.study.dto.StudyAttendeeResultDto;
import inha.gdgoc.domain.study.dto.StudyDto;
import inha.gdgoc.domain.study.dto.StudyListWithMetaDto;
import inha.gdgoc.domain.study.dto.request.StudyCreateRequest;
import inha.gdgoc.domain.study.dto.response.GetCreatorResponse;
import inha.gdgoc.domain.study.dto.response.GetDetailedStudyResponse;
import inha.gdgoc.domain.study.dto.response.MyStudyRecruitResponse;
import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.entity.StudyAttendee;
import inha.gdgoc.domain.study.enums.AttendeeStatus;
import inha.gdgoc.domain.study.enums.CreatorType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.study.repository.StudyAttendeeRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
@Transactional
class StudyServiceTest {

    @Autowired
    private StudyService studyService;

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


    @DisplayName("해당 스터디 정보를 id로 조회한다.")
    @Test
    void getStudyById() {
        // given
        String findTitle = "테스트제목";
        Study findStudy = createStudy(findTitle, user);
        studyRepository.save(findStudy);

        // when
        GetDetailedStudyResponse resultStudy = studyService.getStudyById(findStudy.getId());

        // then
        assertThat(resultStudy).isNotNull();
        assertThat(resultStudy.creator()).isEqualTo(GetCreatorResponse.from(user));
        assertThat(resultStudy.title()).isEqualTo(findTitle);
    }

    @DisplayName("해당 스터디 id가 없다면 에러가 발생한다.")
    @Test
    void getStudyByIdNotFound() {
        // then
        assertThatThrownBy(() -> {
            studyService.getStudyById(99999L);
        }).isInstanceOf(NotFoundException.class);
    }

    @DisplayName("스터디 목록을 페이징하여 조회한다.")
    @Test
    void getStudyList() {
        // given
        for (int i = 0; i < 15; i++) {
            studyRepository.save(createStudy("스터디" + i, user));
        }

        // when
        StudyListWithMetaDto page_ONE_Result = studyService.getStudyList(
                Optional.of(1L),
                Optional.empty(),
                Optional.empty()
        );

        StudyListWithMetaDto page_TWO_Result = studyService.getStudyList(
                Optional.of(2L),
                Optional.empty(),
                Optional.empty()
        );

        // then
        assertThat(page_ONE_Result).isNotNull();
        assertThat(page_ONE_Result.getStudyList()).hasSize(10);
        assertThat(page_ONE_Result.getPage()).isEqualTo(1L);
        assertThat(page_ONE_Result.getPageCount()).isGreaterThanOrEqualTo(15);

        assertThat(page_TWO_Result).isNotNull();
        assertThat(page_TWO_Result.getStudyList()).hasSize(5);
        assertThat(page_TWO_Result.getPage()).isEqualTo(2L);
        assertThat(page_TWO_Result.getPageCount()).isGreaterThanOrEqualTo(15);
    }

    @DisplayName("page가 1보다 작으면 예외가 발생한다.")
    @Test
    void getStudyListInvalidPage() {
        // then
        assertThatThrownBy(() -> {
            studyService.getStudyList(
                    Optional.of(0L),
                    Optional.empty(),
                    Optional.empty()
            );
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("page가 1보다 작을 수 없습니다");
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
                .creatorType(CreatorType.PERSONAL)
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

    @DisplayName("특정 지원자의 스터디 결과 리스트를 조회한다.")
    @Test
    void getStudyAttendeeResultListByUserId() {
        // given
        String resultTitle_1 = "AI 스터디";
        String resultIntroduce_1 = "AI에 관심 많습니다.";
        String resultActivityTime_1 = "저녁";
        AttendeeStatus resultStatus_1 = AttendeeStatus.APPROVED;

        String resultTitle_2 = "블록체인 스터디";
        String resultIntroduce_2 = "블록체인도 배우고 싶어요.";
        String resultActivityTime_2 = "주말";
        AttendeeStatus resultStatus_2 = AttendeeStatus.REQUESTED;

        Study study1 = createStudy(resultTitle_1, user);
        Study study2 = createStudy(resultTitle_2, user);
        studyRepository.saveAll(List.of(study1, study2));


        StudyAttendee attendee1 = StudyAttendee.builder()
                .study(study1)
                .user(user)
                .status(resultStatus_1)
                .introduce(resultIntroduce_1)
                .activityTime(resultActivityTime_1)
                .build();

        StudyAttendee attendee2 = StudyAttendee.builder()
                .study(study2)
                .user(user)
                .status(resultStatus_2)
                .introduce(resultIntroduce_2)
                .activityTime(resultActivityTime_2)
                .build();

        studyAttendeeRepository.saveAll(List.of(attendee1, attendee2));

        // when
        List<StudyAttendeeResultDto> result = studyAttendeeService.getStudyAttendeeResultListByUserId(user.getId());

        // then
        StudyAttendeeResultDto dto1 = result.get(1);
        StudyAttendeeResultDto dto2 = result.get(0);

        assertThat(result).hasSize(2);
        assertThat(dto1.getStudyId()).isEqualTo(study1.getId());
        assertThat(dto1.getTitle()).isEqualTo(resultTitle_1);
        assertThat(dto1.getStatus()).isEqualTo(resultStatus_1);

        assertThat(dto2.getStudyId()).isEqualTo(study2.getId());
        assertThat(dto2.getTitle()).isEqualTo(resultTitle_2);
        assertThat(dto2.getStatus()).isEqualTo(resultStatus_2);
    }

    @DisplayName("내가 만든 스터디 목록을 모집 상태별로 조회한다.")
    @Test
    void getMyStudyList() {
        // given
        User creator = createUser();
        userRepository.save(creator);

        String find_recruiting_title = "AI 스터디";
        String find_recruited_title = "블록체인 스터디";

        Study recruitingStudy1 = createRecruitStudy(
                find_recruiting_title,
                LocalDateTime.of(2025, 4, 10, 0, 0),
                LocalDateTime.of(2025, 6, 10, 0, 0),
                StudyStatus.RECRUITING,
                creator
        );

        Study recruitedStudy1 = createRecruitStudy(
                find_recruited_title,
                LocalDateTime.of(2025, 3, 1, 0, 0),
                LocalDateTime.of(2025, 4, 30, 0, 0),
                StudyStatus.RECRUITED,
                creator
        );

        studyRepository.saveAll(List.of(recruitingStudy1, recruitedStudy1));

        // when
        MyStudyRecruitResponse response = studyService.getMyStudyList(creator.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRecruiting()).hasSize(1);
        assertThat(response.getRecruiting().get(0).getTitle()).isEqualTo(find_recruiting_title);

        assertThat(response.getRecruited()).hasSize(1);
        assertThat(response.getRecruited().get(0).getTitle()).isEqualTo(find_recruited_title);
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
        return this.createRecruitStudy(title, LocalDateTime.now(), LocalDateTime.now(), StudyStatus.RECRUITED, user);
    }

    private Study createRecruitStudy(
            String title,
            LocalDateTime activityStartDate,
            LocalDateTime activityEndDate,
            StudyStatus status,
            User user
    ) {
        return Study.builder()
                .title(title)
                .simpleIntroduce("간단한 소개")
                .activityIntroduce("활동 소개")
                .imagePath("test url")
                .creatorType(CreatorType.PERSONAL)
                .status(status)
                .expectedTime("매일매일")
                .expectedPlace("인하대정문")
                .recruitStartDate(LocalDateTime.now())
                .recruitEndDate(LocalDateTime.now())
                .activityStartDate(activityStartDate)
                .activityEndDate(activityEndDate)
                .user(user)
                .build();
    }
}