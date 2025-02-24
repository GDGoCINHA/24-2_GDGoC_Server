package inha.gdgoc.domain.recruit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.entity.Answer;
import inha.gdgoc.domain.recruit.entity.RecruitMember;
import inha.gdgoc.domain.recruit.enums.InputType;
import inha.gdgoc.domain.recruit.enums.SurveyType;
import inha.gdgoc.domain.recruit.repository.AnswerRepository;
import inha.gdgoc.domain.recruit.repository.RecruitMemberRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RecruitMemberService {
    private final RecruitMemberRepository recruitMemberRepository;
    private final AnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void addRecruitMember(ApplicationRequest applicationRequest) {
        RecruitMember member = applicationRequest.getMember().toEntity();
        recruitMemberRepository.save(member);

        List<Answer> answers = applicationRequest.getAnswers().entrySet().stream()
                .map(entry -> {
                    try {
                        // Object → JSON String 변환
                        String jsonValue = objectMapper.writeValueAsString(entry.getValue());
                        return new Answer(member, SurveyType.fromType("recruit form"), InputType.fromQuestion(
                                entry.getKey()), jsonValue);
                    } catch (Exception e) {
                        throw new RuntimeException("JSON 변환 오류", e);
                    }
                })
                .toList();

        answerRepository.saveAll(answers);
    }

    public boolean isRegisteredStudentId(String studentId){
        return recruitMemberRepository.existsByStudentId(studentId);
    }

    public boolean isRegisteredPhoneNumber(String phoneNumber) {
        return recruitMemberRepository.existsByPhoneNumber(phoneNumber);
    }
}
