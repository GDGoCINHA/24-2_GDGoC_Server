package inha.gdgoc.domain.recruit.member.service;

import static inha.gdgoc.domain.recruit.member.exception.RecruitMemberErrorCode.RECRUIT_MEMBER_NOT_FOUND;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.member.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.member.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.recruit.member.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.recruit.member.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.member.entity.Answer;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.InputType;
import inha.gdgoc.domain.recruit.member.enums.SurveyType;
import inha.gdgoc.domain.recruit.member.exception.RecruitMemberException;
import inha.gdgoc.domain.recruit.member.repository.AnswerRepository;
import inha.gdgoc.domain.recruit.member.repository.RecruitMemberRepository;
import inha.gdgoc.global.util.SemesterCalculator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class RecruitMemberService {
    private final RecruitMemberRepository recruitMemberRepository;
    private final AnswerRepository answerRepository;
    private final ObjectMapper objectMapper;
    private final SemesterCalculator semesterCalculator;

    @Transactional
    public void addRecruitMember(ApplicationRequest applicationRequest) {
        RecruitMember member = applicationRequest.getMember()
                .toEntity(semesterCalculator.currentSemester());
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

    public CheckStudentIdResponse isRegisteredStudentId(String studentId) {
        boolean exists = recruitMemberRepository.existsByStudentId(studentId);

        return new CheckStudentIdResponse(exists);
    }

    public CheckPhoneNumberResponse isRegisteredPhoneNumber(String phoneNumber) {
        boolean exists = recruitMemberRepository.existsByPhoneNumber(phoneNumber);

        return new CheckPhoneNumberResponse(exists);
    }

    public SpecifiedMemberResponse findSpecifiedMember(Long id) {
        RecruitMember member = recruitMemberRepository.findById(id)
                .orElseThrow(() -> new RecruitMemberException(RECRUIT_MEMBER_NOT_FOUND));
        List<Answer> answers = answerRepository
                .findByRecruitMemberAndSurveyType(member, SurveyType.RECRUIT);

        return SpecifiedMemberResponse.from(member, answers, objectMapper);
    }

    @Transactional
    public void updatePayment(Long memberId, boolean isPayed) {
        RecruitMember m = recruitMemberRepository.findById(memberId)
                .orElseThrow(() -> new RecruitMemberException(RECRUIT_MEMBER_NOT_FOUND));

        if (Boolean.TRUE.equals(m.getIsPayed()) == isPayed) return;

        if (isPayed) m.markPaid();
        else m.markUnpaid();
    }

    @Transactional(readOnly = true)
    public Page<RecruitMember> findAllMembersPage(Pageable pageable) {
        return recruitMemberRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<RecruitMember> searchMembersByNamePage(String name, Pageable pageable) {
        return recruitMemberRepository.findByNameContainingIgnoreCase(name, pageable);
    }

}
