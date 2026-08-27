package inha.gdgoc.domain.recruit.member.service;

import static inha.gdgoc.domain.recruit.member.exception.RecruitMemberErrorCode.RECRUIT_MEMBER_NOT_FOUND;
import static inha.gdgoc.domain.recruit.member.exception.RecruitMemberErrorCode.RECRUIT_MEMBER_ALREADY_APPLIED;
import static inha.gdgoc.domain.recruit.member.exception.RecruitMemberErrorCode.RECRUIT_MEMBER_INVALID_EMAIL_DOMAIN;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.domain.resource.dto.response.PresignedUploadResponse;
import inha.gdgoc.domain.resource.exception.ResourceErrorCode;
import inha.gdgoc.domain.resource.exception.ResourceException;
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
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.recruit.member.enums.InputType;
import inha.gdgoc.domain.recruit.member.enums.SurveyType;
import inha.gdgoc.domain.recruit.member.exception.RecruitMemberException;
import inha.gdgoc.domain.recruit.member.repository.AnswerRepository;
import inha.gdgoc.domain.recruit.member.repository.RecruitMemberMemoRepository;
import inha.gdgoc.domain.recruit.member.repository.RecruitMemberRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import inha.gdgoc.global.util.SemesterCalculator;
import inha.gdgoc.global.util.MajorNormalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class RecruitMemberService {
    private static final long MAX_PROOF_FILE_SIZE = 10 * 1024 * 1024;
    private static final String INHA_EMAIL_DOMAIN = "@inha.edu";

    private final RecruitMemberRepository recruitMemberRepository;
    private final RecruitMemberMemoRepository recruitMemberMemoRepository;
    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final ObjectMapper objectMapper;
    private final SemesterCalculator semesterCalculator;
    private final S3Service s3Service;
    private final MajorNormalizer majorNormalizer;

    /**
     * 지원을 접수한다.
     *
     * <p>신원(이름·학번·이메일·전화·학과)은 폼이 아니라 로그인한 계정에서 가져온다. 가입 때 이미 받은 값이고,
     * 지원서와 계정을 잇는 키가 이메일이라 타이핑에 맡기면 오타 하나로 영영 안 이어진다.
     */
    @Transactional
    public void addRecruitMember(Map<String, Object> requestPayload, MultipartFile file, Long userId) {
        User applicant = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
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

        memberRequest = memberRequest.withIdentity(
                applicant.getName(),
                applicant.getStudentId(),
                applicant.getEmail(),
                applicant.getPhoneNumber(),
                applicant.getMajor());

        log.info("[recruit-member] 지원 접수 - studentId={}, name={}, hasProofFile={}",
                memberRequest.getStudentId(), memberRequest.getName(), file != null && !file.isEmpty());

        if (file != null && !file.isEmpty()) {
            String key;
            try {
                key = uploadProofFile(file);
            } catch (RuntimeException ex) {
                log.warn("[recruit-member] 지원 실패(증빙 파일 업로드) - studentId={}, name={}, reason={}",
                        memberRequest.getStudentId(), memberRequest.getName(), ex.toString());
                throw ex;
            }
            String proofFileUrl = s3Service.getS3FileUrl(key);
            answers.put("proofFileUrl", proofFileUrl);
        }
        normalizeProofFileUrl(answers);

        AdmissionSemester semester = semesterCalculator.currentSemester();
        validateInhaEmail(memberRequest);
        validateNotAppliedThisSemester(memberRequest, semester);

        RecruitMember member = memberRequest.toEntity(semester, majorNormalizer);
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

        log.info("[recruit-member] 지원 완료 - applicationId={}, semester={}, studentId={}, name={}",
                member.getId(), semester, member.getStudentId(), member.getName());
    }

    /** 이메일은 도메인만 남긴다. 어느 도메인으로 잘못 들어오는지가 진단에 필요한 전부다. */
    private String domainOf(String email) {
        if (email == null) {
            return "none";
        }
        int at = email.lastIndexOf('@');
        return at < 0 ? "malformed" : email.substring(at).toLowerCase();
    }

    private String uploadProofFile(MultipartFile file) {
        try {
            return s3Service.upload(0L, S3KeyType.recruitMember, file);
        } catch (Exception e) {
            throw new RuntimeException("증빙 파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    public PresignedUploadResponse createProofFilePresignedUpload(
            String fileName,
            String contentType,
            Long fileSize
    ) {
        if (fileSize == null || fileSize > MAX_PROOF_FILE_SIZE) {
            throw new ResourceException(ResourceErrorCode.INVALID_BIG_FILE);
        }

        S3Service.PresignedUpload presignedUpload = s3Service.createPresignedUpload(
                0L,
                S3KeyType.recruitMember,
                fileName,
                contentType
        );
        return new PresignedUploadResponse(presignedUpload.key(), presignedUpload.uploadUrl());
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

    // 중복 확인 3종은 전부 이번 학기만 본다. 전역으로 보면 지난 학기 지원자가
    // 「중복입니다」에 걸려 이번 학기 지원을 시작조차 못 한다.
    public CheckStudentIdResponse isRegisteredStudentId(String studentId) {
        boolean exists = recruitMemberRepository.existsByStudentIdAndAdmissionSemester(
                studentId, semesterCalculator.currentSemester());

        return new CheckStudentIdResponse(exists);
    }

    public CheckPhoneNumberResponse isRegisteredPhoneNumber(String phoneNumber) {
        String cleanPhone = normalizePhoneNumber(phoneNumber);
        boolean exists = recruitMemberRepository.existsByPhoneNumberAndAdmissionSemester(
                cleanPhone, semesterCalculator.currentSemester());

        return new CheckPhoneNumberResponse(exists);
    }

    public CheckEmailResponse isRegisteredEmail(String email) {
        boolean exists = recruitMemberRepository.existsByEmailIgnoreCaseAndAdmissionSemester(
                email.trim(), semesterCalculator.currentSemester());

        return new CheckEmailResponse(exists);
    }

    public SpecifiedMemberResponse findSpecifiedMember(Long id) {
        RecruitMember member = recruitMemberRepository.findById(id)
                .orElseThrow(() -> new RecruitMemberException(RECRUIT_MEMBER_NOT_FOUND));

        return toSpecifiedMemberResponse(member);
    }

    /**
     * 로그인 사용자가 <b>이번 학기</b>에 낸 부원 지원서를 돌려준다. 마이페이지에서 쓴다.
     *
     * <p>부원 지원은 비로그인으로 받아 계정과 이어지는 컬럼이 없다. 이메일이 유일한 연결 고리이며,
     * 로그인은 @inha.edu 계정만, 지원 폼의 도메인도 @inha.edu 고정이라 이 매칭이 성립한다.
     */
    @Transactional(readOnly = true)
    public SpecifiedMemberResponse findMyApplication(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
        AdmissionSemester semester = semesterCalculator.currentSemester();
        RecruitMember member = recruitMemberRepository
                .findByEmailIgnoreCaseAndAdmissionSemester(user.getEmail().trim(), semester)
                .or(() -> findByAccountStudentId(user, semester))
                .orElseThrow(() -> new RecruitMemberException(RECRUIT_MEMBER_NOT_FOUND));

        return toSpecifiedMemberResponse(member);
    }

    /**
     * 이메일로 못 찾았을 때의 대비책.
     *
     * <p>로그인 필수로 바뀌기 전에는 지원 폼에 이메일을 직접 적었다. 자기 계정과 다른 @inha.edu 주소를 적은 사람은
     * 이메일만으로는 영영 안 이어진다.
     *
     * <p>학번만으로 찾으면 오타로 남의 학번을 적은 지원서가 걸릴 수 있어 <b>이름까지 같을 때만</b> 인정한다.
     */
    private Optional<RecruitMember> findByAccountStudentId(User user, AdmissionSemester semester) {
        if (user.getStudentId() == null || user.getStudentId().isBlank()) {
            return Optional.empty();
        }
        String accountName = compactName(user.getName());
        if (accountName.isEmpty()) {
            return Optional.empty();
        }
        return recruitMemberRepository
                .findByStudentIdAndAdmissionSemester(user.getStudentId().trim(), semester)
                .filter(found -> compactName(found.getName()).equals(accountName));
    }

    /** 이름 비교에서 공백만 다른 경우("홍 길동")를 같은 것으로 본다. */
    private String compactName(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "");
    }

    private SpecifiedMemberResponse toSpecifiedMemberResponse(RecruitMember member) {
        List<Answer> answers = answerRepository
                .findByRecruitMemberAndSurveyType(member, SurveyType.RECRUIT);

        return SpecifiedMemberResponse.from(member, answers, objectMapper);
    }

    /**
     * 지원 이메일은 @inha.edu 만 받는다.
     *
     * <p>이 이메일이 지원서와 계정을 잇는 유일한 키다 — 로그인은 @inha.edu 전용이라 다른 도메인으로
     * 지원하면 나중에 로그인해도 영영 이어지지 않고 MEMBER 승격도 안 된다. 화면이 도메인을 고정하지만
     * 지원 API 는 인증 없이 열려 있어 주소를 직접 치면 그대로 들어온다 — 실제 차단은 여기서 한다.
     */
    private void validateInhaEmail(RecruitMemberRequest request) {
        String email = request.getEmail();
        String normalized = (email == null ? "" : email.trim().toLowerCase());
        if (!normalized.endsWith(INHA_EMAIL_DOMAIN)) {
            log.warn("[recruit-member] 지원 거절(이메일 도메인) - domain={}, studentId={}, name={}",
                    domainOf(email), request.getStudentId(), request.getName());
            throw new RecruitMemberException(RECRUIT_MEMBER_INVALID_EMAIL_DOMAIN);
        }
    }

    /**
     * 한 학기에 한 번만 받는다 — 이메일·학번·전화번호 중 하나라도 겹치면 막는다.
     *
     * <p>화면에서도 지원 이력을 먼저 조회해 폼 자체를 막지만 그건 안내일 뿐이다. 주소를 직접 치면
     * 그대로 들어오므로 실제 차단은 여기서 한다.
     *
     * <p>학번·전화번호는 DB 에도 (값, 학기) 복합 UNIQUE 가 있다. 그것만 믿으면 중복 제출이
     * 제약 위반 500 으로 나가므로, 여기서 먼저 걸러 409 로 답한다.
     */
    private void validateNotAppliedThisSemester(RecruitMemberRequest request, AdmissionSemester semester) {
        String email = request.getEmail();
        if (email != null && !email.isBlank()
                && recruitMemberRepository.existsByEmailIgnoreCaseAndAdmissionSemester(email.trim(), semester)) {
            rejectAsDuplicate("email", semester, request);
        }

        String studentId = request.getStudentId();
        if (studentId != null && !studentId.isBlank()
                && recruitMemberRepository.existsByStudentIdAndAdmissionSemester(studentId.trim(), semester)) {
            rejectAsDuplicate("studentId", semester, request);
        }

        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.isBlank()
                && recruitMemberRepository.existsByPhoneNumberAndAdmissionSemester(
                        normalizePhoneNumber(phoneNumber), semester)) {
            rejectAsDuplicate("phoneNumber", semester, request);
        }
    }

    /** 세 검사가 같은 에러코드를 던지므로, 어느 필드가 겹쳤는지는 로그에만 남는다. */
    private void rejectAsDuplicate(String field, AdmissionSemester semester, RecruitMemberRequest request) {
        log.warn("[recruit-member] 지원 거절(중복) - field={}, semester={}, studentId={}, name={}",
                field, semester, request.getStudentId(), request.getName());
        throw new RecruitMemberException(RECRUIT_MEMBER_ALREADY_APPLIED);
    }

    @Transactional
    public void updatePayment(Long memberId, boolean isPayed) {
        RecruitMember m = recruitMemberRepository.findById(memberId)
                .orElseThrow(() -> new RecruitMemberException(RECRUIT_MEMBER_NOT_FOUND));

        if (Boolean.TRUE.equals(m.getIsPayed()) == isPayed) return;

        if (isPayed) m.markPaid();
        else m.markUnpaid();

        syncRoleWithPayment(m, isPayed);
    }

    /**
     * 입금 체크를 계정 역할에 곧바로 반영한다.
     *
     * <p>승격 지점이 여기와 로그인 두 곳인 이유: 지원서에는 계정을 가리키는 컬럼이 없어, 아직 가입하지
     * 않은 지원자는 여기서 올릴 대상이 없다. 그 사람은 첫 로그인 때 AuthService 가 올린다. 반대로
     * 이미 가입한 사람은 여기서 올려야 재로그인 없이 바로 글을 쓸 수 있다. 두 자리가 서로의 빈틈을 메운다.
     *
     * <p><b>올릴 때는 GUEST 만, 내릴 때는 MEMBER 만</b> 건드린다. CORE 이상은 회비와 무관하게 임명된
     * 자리라, 잘못 누른 체크 한 번으로 운영진이 GUEST 로 떨어지면 안 된다. GUEST·MEMBER 는 팀을 갖지
     * 않으므로(isTeamAssignableRole 은 CORE·LEAD 만) 팀은 건드릴 것이 없다.
     *
     * <p>지난 학기 지원서의 입금 체크로 이번 학기 권한이 움직여서는 안 되므로 학기를 먼저 본다.
     */
    private void syncRoleWithPayment(RecruitMember application, boolean isPayed) {
        if (application.getAdmissionSemester() != semesterCalculator.currentSemester()) {
            return;
        }

        String email = application.getEmail();
        if (email == null || email.isBlank()) {
            return;
        }

        for (User user : userRepository.findByEmailIgnoreCase(email.trim())) {
            if (isPayed && user.getUserRole() == UserRole.GUEST) {
                user.changeRole(UserRole.MEMBER);
                userRepository.save(user);
            } else if (!isPayed && user.getUserRole() == UserRole.MEMBER) {
                user.changeRole(UserRole.GUEST);
                userRepository.save(user);
            }
        }
    }

    /**
     * 이름 검색과 지원 학기를 조합해 조회한다. 둘 다 선택이며 없으면 전체를 본다.
     *
     * <p>조건이 늘 때마다 쿼리 메서드가 조합 수만큼 불어나므로 Specification 으로 둔다.
     * 관리자 코어 지원서 조회(RecruitCoreAdminService)와 같은 방식이다.
     */
    @Transactional(readOnly = true)
    public Page<RecruitMember> searchMembers(
            String name,
            AdmissionSemester admissionSemester,
            Pageable pageable
    ) {
        Specification<RecruitMember> spec = (root, query, builder) -> builder.conjunction();

        if (name != null && !name.isBlank()) {
            String keyword = "%" + name.trim().toLowerCase() + "%";
            spec = spec.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("name")), keyword));
        }
        if (admissionSemester != null) {
            spec = spec.and((root, query, builder) ->
                    builder.equal(root.get("admissionSemester"), admissionSemester));
        }

        return recruitMemberRepository.findAll(spec, pageable);
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
        putIfPresent(answers, "proofFileUrl", step6.get("proofFileUrl"));

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
        putIfPresent(answers, "proofFileUrl", rawAnswers.get("proofFileUrl"));
        return answers;
    }

    private void normalizeProofFileUrl(Map<String, Object> answers) {
        Object proofFileUrl = answers.get("proofFileUrl");
        if (!(proofFileUrl instanceof String fileUrl) || fileUrl.isBlank()) {
            return;
        }
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            return;
        }
        answers.put("proofFileUrl", s3Service.getS3FileUrl(fileUrl));
    }

}
