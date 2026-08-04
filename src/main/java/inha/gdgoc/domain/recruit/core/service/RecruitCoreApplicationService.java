package inha.gdgoc.domain.recruit.core.service;

import inha.gdgoc.domain.recruit.core.config.RecruitCoreSessionResolver;
import inha.gdgoc.domain.recruit.core.dto.request.RecruitCoreApplicationCreateRequest;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicantDetailResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicationCreateResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreEligibilityResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreMyApplicationResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCorePrefillResponse;
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCorePeriodStatus;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreAlreadyAppliedException;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreApplicationNotFoundException;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreClosedException;
import inha.gdgoc.domain.recruit.core.exception.RecruitCoreNotOpenException;
import inha.gdgoc.domain.recruit.core.repository.RecruitCoreApplicationRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import inha.gdgoc.global.util.MajorNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruitCoreApplicationService {

    private final RecruitCoreApplicationRepository repository;
    private final UserRepository userRepository;
    private final RecruitCoreSessionResolver recruitCoreSessionResolver;
    private final MajorNormalizer majorNormalizer;
    private final S3Service s3Service;
    private final Instant openAt;
    private final Instant closeAt;
    private final Clock clock;

    // 시각을 주입받는다. `Instant.now()` 를 직접 부르면 마감일이 지나는 순간
    // 테스트가 통째로 무너지고, 마감 동작 자체를 검증할 방법도 없다.
    // SemesterCalculator·RecruitCoreSessionResolver 와 같은 방식이다.
    //
    // 기간도 코드가 아니라 설정에서 받는다. 코드에 박으면 마감일이 지나는 순간
    // 아무도 모르게 API 가 막힌다 — 실제로 2026-03-14 부터 5개월간 그랬다.
    // 다음 모집은 환경변수만 바꾸면 된다.
    //
    // Instant 가 아니라 String 으로 받는 이유: Instant.parse() 는 'Z' 형식만 받아
    // '+09:00' 을 거부한다. .env 에 KST 를 그대로 적을 수 있어야 오프셋 계산 실수가 안 난다.
    @Autowired
    public RecruitCoreApplicationService(
        RecruitCoreApplicationRepository repository,
        UserRepository userRepository,
        RecruitCoreSessionResolver recruitCoreSessionResolver,
        MajorNormalizer majorNormalizer,
        S3Service s3Service,
        @Value("${app.recruit.core.open-at}") String openAt,
        @Value("${app.recruit.core.close-at}") String closeAt
    ) {
        this(repository, userRepository, recruitCoreSessionResolver, majorNormalizer, s3Service,
            OffsetDateTime.parse(openAt).toInstant(),
            OffsetDateTime.parse(closeAt).toInstant(),
            Clock.system(ZoneId.of("Asia/Seoul")));
    }

    public RecruitCoreApplicationService(
        RecruitCoreApplicationRepository repository,
        UserRepository userRepository,
        RecruitCoreSessionResolver recruitCoreSessionResolver,
        MajorNormalizer majorNormalizer,
        S3Service s3Service,
        Instant openAt,
        Instant closeAt,
        Clock clock
    ) {
        if (!openAt.isBefore(closeAt)) {
            throw new IllegalArgumentException(
                "app.recruit.core.open-at 은 close-at 보다 앞서야 한다: openAt=" + openAt
                    + ", closeAt=" + closeAt);
        }
        this.repository = repository;
        this.userRepository = userRepository;
        this.recruitCoreSessionResolver = recruitCoreSessionResolver;
        this.majorNormalizer = majorNormalizer;
        this.s3Service = s3Service;
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RecruitCoreApplicantDetailResponse getApplicantDetail(Long id) {
        RecruitCoreApplication app = getApplication(id);
        return RecruitCoreApplicantDetailResponse.from(app, toS3FileUrls(app.getFileUrls()));
    }

    @Transactional(readOnly = true)
    public RecruitCoreEligibilityResponse checkEligibility(Long userId) {
        validateRecruitmentOpen();
        String session = recruitCoreSessionResolver.currentSession();
        return repository.findByUserIdAndSession(userId, session)
            .map(app -> RecruitCoreEligibilityResponse.ineligible(session, "ALREADY_APPLIED", app.getId()))
            .orElseGet(() -> RecruitCoreEligibilityResponse.eligible(session));
    }

    @Transactional(readOnly = true)
    public RecruitCorePrefillResponse prefill(Long userId) {
        validateRecruitmentOpen();
        String session = recruitCoreSessionResolver.currentSession();
        repository.findByUserIdAndSession(userId, session)
            .ifPresent(existing -> {
                throw new RecruitCoreAlreadyAppliedException(session, existing.getId());
            });

        User user = getUser(userId);
        return RecruitCorePrefillResponse.from(user);
    }

    @Transactional
    public RecruitCoreApplicationCreateResponse submit(Long userId, RecruitCoreApplicationCreateRequest request) {
        validateRecruitmentOpen();
        String session = recruitCoreSessionResolver.currentSession();
        repository.findByUserIdAndSession(userId, session)
            .ifPresent(existing -> {
                throw new RecruitCoreAlreadyAppliedException(session, existing.getId());
            });

        User user = getUser(userId);
        List<String> fileUrls = request.fileUrls() == null
            ? List.of()
            : List.copyOf(request.fileUrls());
        String cleanPhone = request.snapshot().phone().replaceAll("[^0-9]", "");
        RecruitCoreApplication application = RecruitCoreApplication.builder()
            .user(user)
            .session(session)
            .name(request.snapshot().name())
            .studentId(request.snapshot().studentId())
            .phone(cleanPhone)
            .major(majorNormalizer.normalize(request.snapshot().major()))
            .email(request.snapshot().email())
            .team(request.team())
            .motivation(request.motivation())
            .wish(request.wish())
            .strengths(request.strengths())
            .pledge(request.pledge())
            .fileUrls(fileUrls)
            .resultStatus(RecruitCoreResultStatus.SUBMITTED)
            .build();

        RecruitCoreApplication saved = repository.save(application);
        return RecruitCoreApplicationCreateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public RecruitCoreMyApplicationResponse getMyApplication(Long userId) {
        String session = recruitCoreSessionResolver.currentSession();
        RecruitCoreApplication application = repository.findByUserIdAndSession(userId, session)
            .orElseThrow(RecruitCoreApplicationNotFoundException::new);
        return RecruitCoreMyApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public RecruitCoreApplicantDetailResponse getApplicantDetailForViewer(
        Long applicationId,
        Long viewerId,
        UserRole viewerRole
    ) {
        RecruitCoreApplication application = repository.findById(applicationId)
            .orElseThrow(RecruitCoreApplicationNotFoundException::new);
        boolean privileged = UserRole.hasAtLeast(viewerRole, UserRole.LEAD);
        if (!privileged && !application.isOwnedBy(viewerId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
        }
        return RecruitCoreApplicantDetailResponse.from(application, toS3FileUrls(application.getFileUrls()));
    }

    private List<String> toS3FileUrls(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return List.of();
        }
        return fileKeys.stream()
            .map(this::toS3FileUrl)
            .toList();
    }

    private String toS3FileUrl(String fileKey) {
        if (fileKey.startsWith("http://") || fileKey.startsWith("https://")) {
            return fileKey;
        }
        return s3Service.getS3FileUrl(fileKey);
    }

    private RecruitCoreApplication getApplication(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateRecruitmentOpen() {
        RecruitCorePeriodStatus status = getPeriodStatus();
        if (status == RecruitCorePeriodStatus.BEFORE_OPEN) {
            throw new RecruitCoreNotOpenException(openAt);
        }
        if (status == RecruitCorePeriodStatus.CLOSED) {
            throw new RecruitCoreClosedException(closeAt);
        }
    }

    public RecruitCorePeriodStatus getPeriodStatus() {
        Instant now = Instant.now(clock);
        if (now.isBefore(openAt)) {
            return RecruitCorePeriodStatus.BEFORE_OPEN;
        }
        if (now.isAfter(closeAt)) {
            return RecruitCorePeriodStatus.CLOSED;
        }
        return RecruitCorePeriodStatus.OPEN;
    }

}
