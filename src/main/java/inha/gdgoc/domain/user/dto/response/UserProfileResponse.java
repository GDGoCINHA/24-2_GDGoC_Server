package inha.gdgoc.domain.user.dto.response;

import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String studentId,
        String major,
        String phoneNumber,
        UserRole userRole,
        TeamType team,
        User.MembershipStatus membershipStatus,
        String image
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getStudentId(),
                user.getMajor(),
                user.getPhoneNumber(),
                user.getUserRole(),
                user.getTeam(),
                user.getMembershipStatus(),
                user.getImage()
        );
    }
}
