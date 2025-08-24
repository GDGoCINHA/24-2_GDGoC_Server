package inha.gdgoc.domain.recruit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.dto.request.RecruitMemberRequest;
import inha.gdgoc.domain.recruit.entity.RecruitMember;
import inha.gdgoc.domain.recruit.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.enums.Gender;
import inha.gdgoc.domain.recruit.repository.AnswerRepository;
import inha.gdgoc.domain.recruit.repository.RecruitMemberRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;

class RecruitMemberServiceTest {

    @Test
    void addMember_ShouldSaveRecruitMemberAndAnswers() {
        // given
        RecruitMemberRequest recruitMemberRequest = RecruitMemberRequest.builder()
                .name("김소연")
                .grade("4")
                .studentId("122123388")
                .enrolledClassification("정등록")
                .phoneNumber("010-1111-2332")
                .nationality("대한민국")
                .email("abc@gmail.com")
                .gender("여자")
                .birth(LocalDate.of(2002, 8, 18))
                .major("컴퓨터공학과")
                .doubleMajor("복수전공 인공지능공학과")
                .isPayed(true)
                .build();

        Map<String, Object> answers = Map.of(
                "gdgUserMotive", "그냥",
                "gdgUserStory", "삶",
                "gdgInterest", List.of("FrontEnd", "BackEnd"),
                "gdgPeriod", List.of("23-1", "24-1"),
                "gdgRoute", "에타",
                "gdgExpect", List.of("djqt"),
                "gdgWish", List.of("a", "b", "c"),
                "gdgFeedback", "asdfsdf"
        );


        RecruitMember savedMember = RecruitMember.builder()
                .id(1L) // 저장될 ID
                .name("김소연")
                .grade("4")
                .studentId("122123388")
                .enrolledClassification(EnrolledClassification.FULL_REGISTRATION)
                .phoneNumber("010-1111-2332")
                .nationality("대한민국")
                .email("abc@gmail.com")
                .gender(Gender.FEMALE)
                .birth(LocalDate.of(2002, 8, 18))
                .major("컴퓨터공학과")
                .doubleMajor("복수전공 인공지능공학과")
                .isPayed(true)
                .build();

    }
}
