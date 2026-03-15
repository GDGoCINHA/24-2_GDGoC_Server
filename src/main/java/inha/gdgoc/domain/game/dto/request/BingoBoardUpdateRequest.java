package inha.gdgoc.domain.game.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BingoBoardUpdateRequest {
    private List<String> marks;
}
