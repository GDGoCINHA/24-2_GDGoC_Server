package inha.gdgoc.domain.user.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Careers implements Serializable {
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String title;
}
