package inha.gdgoc.domain.core.attendance.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

public record SetAttendanceRequest(
        @NotNull List<Long> userIds,
        @NotNull Boolean present
) {
    public List<Long> safeUserIds() {
        return userIds == null ? List.of() : userIds.stream().filter(Objects::nonNull).toList();
    }
    public boolean presentValue() {
        return Boolean.TRUE.equals(present);
    }
}