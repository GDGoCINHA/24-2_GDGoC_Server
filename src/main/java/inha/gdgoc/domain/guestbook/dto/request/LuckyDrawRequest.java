package inha.gdgoc.domain.guestbook.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record LuckyDrawRequest(@Min(1) @Max(50) int count) {

    @JsonCreator
    public LuckyDrawRequest(@JsonProperty("count") Integer count) {
        this((count == null) ? 1 : count);
    }
}
