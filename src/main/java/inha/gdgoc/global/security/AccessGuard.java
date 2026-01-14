package inha.gdgoc.global.security;

import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 중앙 집중 권한 검사기.
 * - {@link #check(Authentication, AccessCondition...)}는 SpEL @PreAuthorize에서 사용.
 * - {@link #require(CustomUserDetails, AccessCondition...)}는 서비스/컨트롤러에서 명시적으로 사용.
 */
@Component("accessGuard")
public class AccessGuard {

    public boolean check(Authentication authentication, AccessCondition... anyOf) {
        CustomUserDetails user = extract(authentication);
        return matches(user, anyOf);
    }

    public boolean check(CustomUserDetails user, AccessCondition... anyOf) {
        return matches(user, anyOf);
    }

    public void require(CustomUserDetails user, AccessCondition... anyOf) {
        if (!matches(user, anyOf)) {
            throw new AccessDeniedException("FORBIDDEN_USER");
        }
    }

    private boolean matches(CustomUserDetails user, AccessCondition... anyOf) {
        if (user == null || anyOf == null || anyOf.length == 0) {
            return false;
        }

        for (AccessCondition condition : anyOf) {
            if (condition != null && condition.matches(user.getRole(), user.getTeam())) {
                return true;
            }
        }

        return false;
    }

    private CustomUserDetails extract(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails user) {
            return user;
        }
        return null;
    }

    public record AccessCondition(UserRole minRole, List<TeamType> teams) {

        public static AccessCondition of(UserRole minRole, List<TeamType> teams) {
            List<TeamType> list = (teams == null || teams.isEmpty())
                    ? List.of()
                    : List.copyOf(teams);
            return new AccessCondition(minRole, list);
        }

        public static AccessCondition of(UserRole minRole, TeamType... teams) {
            if (teams == null || teams.length == 0) {
                return new AccessCondition(minRole, List.of());
            }
            return new AccessCondition(minRole, List.copyOf(Arrays.asList(teams)));
        }

        public static AccessCondition atLeast(UserRole minRole) {
            return of(minRole);
        }

        private boolean matches(UserRole currentRole, TeamType currentTeam) {
            if (minRole != null && !UserRole.hasAtLeast(currentRole, minRole)) {
                return false;
            }
            if (teams.isEmpty()) {
                return true;
            }
            return currentTeam != null && teams.contains(currentTeam);
        }
    }
}
