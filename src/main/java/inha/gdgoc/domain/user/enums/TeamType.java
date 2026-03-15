package inha.gdgoc.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum TeamType {
    HQ("HQ"),
    HR("HR"),
    PR_DESIGN("PR_DESIGN"),
    TECH("TECH"),
    BD("BD");

    private final String label;

    TeamType(String label) { this.label = label; }

    @JsonCreator
    public static TeamType from(String raw) {
        if (raw == null) return null;

        String normalized = raw.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase();

        if (normalized.isBlank()) {
            return null;
        }

        return switch (normalized) {
            case "HQ" -> HQ;
            case "HR" -> HR;
            case "TECH" -> TECH;
            case "BD" -> BD;
            case "PR_DESIGN" -> PR_DESIGN;
            case "PR/DESIGN" -> PR_DESIGN;
            default -> throw new IllegalArgumentException("Unknown team: " + raw);
        };
    }
}
