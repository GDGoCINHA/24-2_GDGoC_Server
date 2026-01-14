package inha.gdgoc.domain.recruit.core.dto.response;

import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;

public record RecruitCoreApplicationSnapshotResponse(
    String name,
    String studentId,
    String phone,
    String major,
    String email
) {

    public static RecruitCoreApplicationSnapshotResponse from(RecruitCoreApplication application) {
        return new RecruitCoreApplicationSnapshotResponse(
            application.getName(),
            application.getStudentId(),
            application.getPhone(),
            application.getMajor(),
            application.getEmail()
        );
    }
}
