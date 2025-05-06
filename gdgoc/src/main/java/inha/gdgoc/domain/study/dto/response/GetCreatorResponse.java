package inha.gdgoc.domain.study.dto.response;

import inha.gdgoc.domain.user.entity.User;

public record GetCreatorResponse(Long id, String name, String major, String studentId, String phoneNumber) {
    public static GetCreatorResponse from(User user) {
        return new GetCreatorResponse(user.getId(), user.getName(), user.getMajor(), user.getStudentId(),
                user.getPhoneNumber());
    }
}
