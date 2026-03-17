package inha.gdgoc.domain.core.attendance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum AttendanceStatus {
    PRESENT("출석"),
    LATE("지각"),
    PRE_ARRANGED("사전 승인"),
    ABSENT("결석");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

    @JsonCreator
    public static AttendanceStatus from(String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase();

        if (normalized.isBlank()) {
            return null;
        }

        return switch (normalized) {
            case "PRESENT" -> PRESENT;
            case "LATE" -> LATE;
            case "PRE_ARRANGED", "PREARRANGED", "AGREED", "EXCUSED" -> PRE_ARRANGED;
            case "ABSENT" -> ABSENT;
            default -> throw new IllegalArgumentException("Unknown attendance status: " + raw);
        };
    }
}
