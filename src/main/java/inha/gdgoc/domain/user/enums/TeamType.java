package inha.gdgoc.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum TeamType {
    HR("HR"),
    PR_DESIGN("PR/DESIGN"),
    TECH("TECH"),
    BD("BD");

    private final String label;

    TeamType(String label) { this.label = label; }

    @JsonCreator
    public static TeamType from(String raw) {
        if (raw == null) return null;
        return switch (raw) {
            case "HR" -> HR;
            case "TECH" -> TECH;
            case "BD" -> BD;
            case "PR/DESIGN" -> PR_DESIGN;
            default -> throw new IllegalArgumentException("Unknown team: " + raw);
        };
    }
}