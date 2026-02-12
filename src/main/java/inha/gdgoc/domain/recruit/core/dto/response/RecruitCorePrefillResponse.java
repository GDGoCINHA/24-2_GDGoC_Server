package inha.gdgoc.domain.recruit.core.dto.response;

import inha.gdgoc.domain.user.entity.User;

public record RecruitCorePrefillResponse(
    String name,
    String studentId,
    String phone,
    String major,
    String email
) {

    public static RecruitCorePrefillResponse from(User user) {
        return new RecruitCorePrefillResponse(
            user.getName(),
            user.getStudentId(),
            user.getPhoneNumber(),
            user.getMajor(),
            user.getEmail()
        );
    }
}
