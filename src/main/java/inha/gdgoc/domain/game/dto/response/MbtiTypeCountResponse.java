package inha.gdgoc.domain.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MbtiTypeCountResponse {
    private final String mbtiType;
    private final long count;
}
