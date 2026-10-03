package inha.gdgoc.domain.club.club.enums;

import java.util.List;

/** PENDING(승인 대기)·REJECTED(반려)·HIDDEN 은 운영진과 그 팀 멤버에게만 보인다. */
public enum ClubStatus {
  PENDING,
  REJECTED,
  ACTIVE,
  ENDED,
  HIDDEN;

  public static final List<ClubStatus> PUBLIC = List.of(ACTIVE, ENDED);

  public boolean isPublic() {
    return PUBLIC.contains(this);
  }
}
