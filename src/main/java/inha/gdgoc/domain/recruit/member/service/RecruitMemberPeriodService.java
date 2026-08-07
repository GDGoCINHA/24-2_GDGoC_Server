package inha.gdgoc.domain.recruit.member.service;

import inha.gdgoc.domain.recruit.member.dto.response.RecruitMemberPeriodResponse;
import inha.gdgoc.domain.recruit.member.enums.RecruitMemberPeriodStatus;
import inha.gdgoc.domain.recruit.member.exception.RecruitMemberErrorCode;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 부원 모집 기간을 판정한다.
 *
 * <p>이 서비스가 생기기 전까지 부원 모집에는 기간 개념이 없었다. 화면은 상수로 일정을 적어 보여주기만 했고 서버는
 * 언제든 지원을 받아, 오픈 전에도 지원이 됐다. 코어(RecruitCoreApplicationService)와 같은 방식으로 맞춘다.
 *
 * <p>시각과 기간을 모두 주입받는 이유도 코어와 같다. {@code Instant.now()} 를 직접 부르면 마감일이 지나는 순간
 * 테스트가 무너지고 마감 동작을 검증할 방법이 없다. 기간을 코드에 박으면 다음 모집 때 배포가 필요하다.
 *
 * <p>Instant 가 아니라 String 으로 받는 이유: {@code Instant.parse()} 는 'Z' 형식만 받아 '+09:00' 을
 * 거부한다. 설정에 KST 를 그대로 적을 수 있어야 오프셋 계산 실수가 안 난다.
 */
@Service
public class RecruitMemberPeriodService {

    private static final DateTimeFormatter KOREAN_DATE =
        DateTimeFormatter.ofPattern("M월 d일 H시");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final Instant openAt;
    private final Instant closeAt;
    private final Clock clock;

    @Autowired
    public RecruitMemberPeriodService(
        @Value("${app.recruit.member.open-at}") String openAt,
        @Value("${app.recruit.member.close-at}") String closeAt
    ) {
        this(
            OffsetDateTime.parse(openAt).toInstant(),
            OffsetDateTime.parse(closeAt).toInstant(),
            Clock.system(KST));
    }

    public RecruitMemberPeriodService(Instant openAt, Instant closeAt, Clock clock) {
        if (!openAt.isBefore(closeAt)) {
            throw new IllegalArgumentException(
                "app.recruit.member.open-at 은 close-at 보다 앞서야 한다: openAt=" + openAt
                    + ", closeAt=" + closeAt);
        }
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.clock = clock;
    }

    public RecruitMemberPeriodResponse getPeriod() {
        return new RecruitMemberPeriodResponse(openAt, closeAt, getPeriodStatus());
    }

    public RecruitMemberPeriodStatus getPeriodStatus() {
        Instant now = Instant.now(clock);
        if (now.isBefore(openAt)) {
            return RecruitMemberPeriodStatus.BEFORE_OPEN;
        }
        if (now.isAfter(closeAt)) {
            return RecruitMemberPeriodStatus.CLOSED;
        }
        return RecruitMemberPeriodStatus.OPEN;
    }

    /**
     * 기간 밖이면 막는다.
     *
     * <p>화면에서도 카드를 잠그지만 그건 안내일 뿐이다. 지원 API 는 인증 없이 열려 있어 주소를 직접 치면 그대로
     * 들어온다 — 실제 차단은 여기서 한다.
     */
    public void validateOpen() {
        RecruitMemberPeriodStatus status = getPeriodStatus();
        if (status == RecruitMemberPeriodStatus.BEFORE_OPEN) {
            throw new BusinessException(
                RecruitMemberErrorCode.RECRUIT_MEMBER_NOT_OPEN,
                "부원 모집은 " + format(openAt) + "에 시작됩니다.");
        }
        if (status == RecruitMemberPeriodStatus.CLOSED) {
            throw new BusinessException(
                RecruitMemberErrorCode.RECRUIT_MEMBER_CLOSED,
                "부원 모집이 " + format(closeAt) + "에 마감되었습니다.");
        }
    }

    /** 오류 문구에 쓸 KST 표기. 보는 사람의 시간대와 무관하게 한국 시각으로 적는다. */
    private String format(Instant instant) {
        return ZonedDateTime.ofInstant(instant, KST).format(KOREAN_DATE);
    }
}
