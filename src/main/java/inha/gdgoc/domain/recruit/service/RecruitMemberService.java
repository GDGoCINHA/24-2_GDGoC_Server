package inha.gdgoc.domain.recruit.service;

import static inha.gdgoc.domain.recruit.exception.RecruitMemberErrorCode.RECRUIT_MEMBER_NOT_FOUND;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.recruit.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.recruit.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.entity.Answer;
import inha.gdgoc.domain.recruit.entity.RecruitMember;
import inha.gdgoc.domain.recruit.enums.InputType;
import inha.gdgoc.domain.recruit.enums.SurveyType;
import inha.gdgoc.domain.recruit.exception.RecruitMemberException;
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

    /**
     * 모집 지원 정보를 저장한다.
     *
     * <p>요청에 포함된 지원자 정보(ApplicationRequest.member)를 RecruitMember 엔티티로 변환하여 저장하고,
     * 요청의 답변 맵(ApplicationRequest.answers)의 각 항목을 JSON 문자열로 직렬화하여 Answer 엔티티 목록을 생성한 뒤 일괄 저장한다.
     *
     * @param applicationRequest 저장할 지원자 정보와 답변을 담은 요청 객체
     * @throws RuntimeException JSON 직렬화 오류 발생 시 ("JSON 변환 오류" 메시지와 원인 예외 포함)
     */
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

    /**
     * 주어진 학번이 이미 등록되어 있는지 확인한다.
     *
     * @param studentId 확인할 학번
     * @return 등록 여부를 담은 CheckStudentIdResponse (exists=true이면 등록된 학번)
     */
    public CheckStudentIdResponse isRegisteredStudentId(String studentId) {
        boolean exists = recruitMemberRepository.existsByStudentId(studentId);

        return new CheckStudentIdResponse(exists);
    }

    /**
     * 주어진 휴대전화 번호가 이미 등록되어 있는지 확인하고 그 결과를 반환합니다.
     *
     * @param phoneNumber 확인할 휴대전화 번호
     * @return 등록 여부를 담은 {@link CheckPhoneNumberResponse}
     */
    public CheckPhoneNumberResponse isRegisteredPhoneNumber(String phoneNumber) {
        boolean exists = recruitMemberRepository.existsByPhoneNumber(phoneNumber);

        return new CheckPhoneNumberResponse(exists);
    }

    /**
     * 주어진 ID에 해당하는 모집 멤버와 해당 멤버의 모집 설문 응답을 조회하여 지정된 응답 DTO로 변환해 반환합니다.
     *
     * 조회된 멤버가 존재하면 해당 멤버를 기준으로 SurveyType.RECRUIT에 해당하는 Answer 목록을 함께 조회하고,
     * ObjectMapper를 이용해 SpecifiedMemberResponse를 생성하여 반환합니다.
     *
     * @return 조회된 멤버와 응답을 조합한 {@link SpecifiedMemberResponse}
     * @throws RecruitMemberException 멤버를 찾을 수 없을 경우(상수 RECRUIT_MEMBER_NOT_FOUND)
     */
    public SpecifiedMemberResponse findSpecifiedMember(Long id) {
        RecruitMember member = recruitMemberRepository.findById(id)
                .orElseThrow(() -> new RecruitMemberException(RECRUIT_MEMBER_NOT_FOUND));
        List<Answer> answers = answerRepository
                .findByRecruitMemberAndSurveyType(member, SurveyType.RECRUIT);

        return SpecifiedMemberResponse.from(member, answers, objectMapper);
    }
}
