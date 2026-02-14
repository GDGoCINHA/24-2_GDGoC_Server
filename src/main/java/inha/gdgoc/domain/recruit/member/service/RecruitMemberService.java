package inha.gdgoc.domain.recruit.member.service;

import static inha.gdgoc.domain.recruit.member.exception.RecruitMemberErrorCode.RECRUIT_MEMBER_NOT_FOUND;
import static inha.gdgoc.domain.recruit.member.exception.RecruitMemberErrorCode.RECRUIT_MEMBER_ALREADY_APPLIED;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.recruit.member.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.member.dto.request.RecruitMemberMemoRequest;
import inha.gdgoc.domain.recruit.member.dto.request.RecruitMemberRequest;
import inha.gdgoc.domain.recruit.member.dto.response.CheckEmailResponse;
import inha.gdgoc.domain.recruit.member.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.recruit.member.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.recruit.member.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.member.entity.Answer;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.InputType;
import inha.gdgoc.domain.recruit.member.enums.SurveyType;
import inha.gdgoc.domain.recruit.member.exception.RecruitMemberException;
import inha.gdgoc.domain.recruit.member.repository.AnswerRepository;
import inha.gdgoc.domain.recruit.member.repository.RecruitMemberMemoRepository;
import inha.gdgoc.domain.recruit.member.repository.RecruitMemberRepository;
import inha.gdgoc.global.util.SemesterCalculator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class RecruitMemberService {
    private final RecruitMemberRepository recruitMemberRepository;
    private final RecruitMemberMemoRepository recruitMemberMemoRepository;
    private final AnswerRepository answerRepository;
    private final ObjectMapper objectMapper;
    private final SemesterCalculator semesterCalculator;
    private final S3Service s3Service;

    @Transactional
    public void addRecruitMember(Map<String, Object> requestPayload, MultipartFile file) {
        RecruitMemberRequest memberRequest;
        Map<String, Object> answers;

        if (requestPayload.containsKey("member")) {
            ApplicationRequest applicationRequest = objectMapper.convertValue(requestPayload, ApplicationRequest.class);
            memberRequest = applicationRequest.getMember();
            answers = normalizeAnswers(applicationRequest.getAnswers());
        } else {
            memberRequest = buildMemberFromNumberedPayload(requestPayload);
            answers = buildAnswersFromNumberedPayload(requestPayload);
        }

        if (file != null && !file.isEmpty()) {
            String key = uploadProofFile(file);
            String proofFileUrl = s3Service.getS3FileUrl(key);
            answers.put("proofFileUrl", proofFileUrl);
        }

        RecruitMember member = memberRequest
                .toEntity(semesterCalculator.currentSemester());
        recruitMemberRepository.save(member);

        List<Answer> answerEntities = answers.entrySet().stream()
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

        answerRepository.saveAll(answerEntities);
    }

    private String uploadProofFile(MultipartFile file) {
        try {
            return s3Service.upload(0L, S3KeyType.study, file);
        } catch (Exception e) {
            throw new RuntimeException("증빙 파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public void addRecruitMemberMemo(RecruitMemberMemoRequest recruitMemberMemoRequest) {
        String cleanPhone = normalizePhoneNumber(recruitMemberMemoRequest.getPhoneNumber());
        boolean alreadyApplied = recruitMemberRepository.existsByPhoneNumber(cleanPhone);
        boolean alreadyMemoRequested = recruitMemberMemoRepository.existsByPhoneNumber(cleanPhone);

        if (alreadyApplied || alreadyMemoRequested) {
            throw new RecruitMemberException(RECRUIT_MEMBER_ALREADY_APPLIED);
        }

        recruitMemberMemoRepository.save(recruitMemberMemoRequest.toEntity());
    }

    public CheckStudentIdResponse isRegisteredStudentId(String studentId) {
        boolean exists = recruitMemberRepository.existsByStudentId(studentId);

        return new CheckStudentIdResponse(exists);
    }

    public CheckPhoneNumberResponse isRegisteredPhoneNumber(String phoneNumber) {
        String cleanPhone = normalizePhoneNumber(phoneNumber);
        boolean exists = recruitMemberRepository.existsByPhoneNumber(cleanPhone);

        return new CheckPhoneNumberResponse(exists);
    }

    public CheckEmailResponse isRegisteredEmail(String email) {
        boolean exists = recruitMemberRepository.existsByEmailIgnoreCase(email.trim());

        return new CheckEmailResponse(exists);
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

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    @SuppressWarnings("unchecked")
    private RecruitMemberRequest buildMemberFromNumberedPayload(Map<String, Object> payload) {
        Map<String, Object> step2 = asMap(payload.get("2"));
        Map<String, Object> step3 = asMap(payload.get("3"));
        Map<String, Object> step4 = asMap(payload.get("4"));
        Map<String, Object> step5 = asMap(payload.get("5"));
        Map<String, Object> step6 = asMap(payload.get("6"));

        Map<String, Object> member = new HashMap<>();
        member.put("name", step2.get("name"));
        member.put("studentId", step2.get("studentId"));
        member.put("enrolledClassification", step2.get("enrolledClassification"));
        member.put("phoneNumber", step3.get("phoneNumber"));
        member.put("email", step4.get("email"));
        member.put("gender", step4.get("gender"));
        member.put("birth", step4.get("birth"));
        member.put("major", step5.get("major"));
        member.put("isPayed", step6.getOrDefault("isPayed", false));

        return objectMapper.convertValue(member, RecruitMemberRequest.class);
    }

    private Map<String, Object> buildAnswersFromNumberedPayload(Map<String, Object> payload) {
        Map<String, Object> step6 = asMap(payload.get("6"));

        Map<String, Object> answers = new HashMap<>();
        putIfPresent(answers, "gdgInterest", step6.get("gdgInterest"));
        putIfPresent(answers, "gdgWish", step6.get("gdgWish"));
        putIfPresent(answers, "gdgFeedback", step6.get("gdgFeedback"));

        return answers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> raw) {
            return (Map<String, Object>) raw;
        }
        return Map.of();
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private Map<String, Object> normalizeAnswers(Map<String, Object> rawAnswers) {
        Map<String, Object> answers = new HashMap<>();
        if (rawAnswers == null) {
            return answers;
        }
        putIfPresent(answers, "gdgInterest", rawAnswers.get("gdgInterest"));
        putIfPresent(answers, "gdgWish", rawAnswers.get("gdgWish"));
        putIfPresent(answers, "gdgFeedback", rawAnswers.get("gdgFeedback"));
        return answers;
    }

}
