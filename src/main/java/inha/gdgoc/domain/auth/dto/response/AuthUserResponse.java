package inha.gdgoc.domain.auth.dto.response;

import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthUserResponse {
    private Long id;
    private String name;
    private String email;
    private UserRole userRole;
    private TeamType team;
    private User.MembershipStatus membershipStatus;
    private String image;

    public static AuthUserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return AuthUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .userRole(user.getUserRole())
                .team(user.getTeam())
                .membershipStatus(user.getMembershipStatus())
                .image(user.getImage())
                .build();
    }
}
