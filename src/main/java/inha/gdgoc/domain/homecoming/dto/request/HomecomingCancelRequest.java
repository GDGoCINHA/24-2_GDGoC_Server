package inha.gdgoc.domain.homecoming.dto.request;

import jakarta.validation.constraints.Size;

public record HomecomingCancelRequest(@Size(max = 200) String reason) {}
