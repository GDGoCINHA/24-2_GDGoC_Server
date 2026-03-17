package inha.gdgoc.domain.core.attendance.dto.request;

import inha.gdgoc.domain.core.attendance.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

public record SetAttendanceRequest(
        @NotNull List<Long> userIds,
        @NotNull AttendanceStatus status
) {
    public List<Long> safeUserIds() {
        return userIds == null ? List.of() : userIds.stream().filter(Objects::nonNull).toList();
    }

    public AttendanceStatus statusValue() {
        return status;
    }
}
