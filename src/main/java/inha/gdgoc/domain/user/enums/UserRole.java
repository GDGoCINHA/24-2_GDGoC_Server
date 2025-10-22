package inha.gdgoc.domain.user.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    GUEST("GUEST"), MEMBER("MEMBER"), CORE("CORE"), LEAD("LEAD"), ORGANIZER("ORGANIZER"), ADMIN("ADMIN");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    /**
     * 나(me)가 required 이상 권한인지
     */
    public static boolean hasAtLeast(UserRole me, UserRole required) {
        if (me == null || required == null) return false;
        return me.rank() >= required.rank();
    }

    /**
     * 역할 서열(낮음→높음). enum 순서 바뀌어도 여기만 수정하면 됨
     */
    public int rank() {
        return switch (this) {
            case GUEST -> 0;
            case MEMBER -> 1;
            case CORE -> 2;
            case LEAD -> 3;
            case ORGANIZER -> 4;
            case ADMIN -> 5;
        };
    }
}