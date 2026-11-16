package inha.gdgoc.domain.homecoming.service;

import static inha.gdgoc.domain.homecoming.exception.HomecomingErrorCode.*;

import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.HomecomingMemberRole;
import inha.gdgoc.domain.homecoming.enums.HomecomingTeam;
import inha.gdgoc.global.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 참석 유형에 따라 받는 칸을 판정하고 나머지는 비운다. 신청과 관리자 수정이 같은 규칙을 쓴다.
 *
 * <p>받지 않는 칸에 값이 와도 거절하지 않고 버린다. 화면에서 유형을 바꾸면 이전 값이 남은 채 올 수 있는데, 그건 신청자 잘못이 아니다.
 */
@Component
public class HomecomingProfileNormalizer {

  static final int MIN_YEAR = 2021;
  static final int MAX_YEAR = 2026;
  static final List<String> ARRIVAL_TIMES = List.of("14:00", "15:00", "16:00", "17:00", "18:00");

  public HomecomingProfile normalize(
      AttendType attendType,
      String rawName,
      String rawPhone,
      Integer yearStart,
      Integer yearEnd,
      HomecomingMemberRole memberRole,
      HomecomingTeam team,
      Boolean isLate,
      String arrivalTime,
      String message) {
    String name = rawName == null ? "" : rawName.strip();
    if (name.isEmpty()) throw new BusinessException(NAME_REQUIRED);
    String phone = formatPhone(rawPhone);

    Integer ys = null, ye = null;
    HomecomingMemberRole role = null;
    HomecomingTeam tm = null;
    if (attendType.hasActivity()) {
      if (yearStart == null || yearEnd == null || memberRole == null) {
        throw new BusinessException(ACTIVITY_REQUIRED);
      }
      if (outOfRange(yearStart) || outOfRange(yearEnd)) throw new BusinessException(YEAR_OUT_OF_RANGE);
      if (yearEnd < yearStart) throw new BusinessException(YEAR_ORDER_INVALID);
      ys = yearStart;
      ye = yearEnd;
      role = memberRole;
      if (role == HomecomingMemberRole.ORGANIZER) {
        if (team == null) throw new BusinessException(TEAM_REQUIRED);
        tm = team;
      }
    }

    boolean late = false;
    String arrival = null;
    String msg = null;
    if (attendType.hasExtra()) {
      late = Boolean.TRUE.equals(isLate);
      if (late) {
        if (arrivalTime == null || !ARRIVAL_TIMES.contains(arrivalTime)) {
          throw new BusinessException(ARRIVAL_TIME_INVALID);
        }
        arrival = arrivalTime;
      }
      if (message != null && !message.isBlank()) msg = message.strip();
    }

    return new HomecomingProfile(attendType, name, phone, ys, ye, role, tm, late, arrival, msg);
  }

  /** 하이픈 유무와 관계없이 {@code 010-0000-0000} 으로 맞춘다. 중복 판정이 이 형태로 비교한다. */
  static String formatPhone(String raw) {
    String d = raw == null ? "" : raw.replaceAll("\\D", "");
    if (!d.matches("^010\\d{8}$")) {
      throw new BusinessException(PHONE_INVALID);
    }
    return d.substring(0, 3) + "-" + d.substring(3, 7) + "-" + d.substring(7);
  }

  private static boolean outOfRange(int year) {
    return year < MIN_YEAR || year > MAX_YEAR;
  }
}
