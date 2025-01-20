package inha.gdgoc.domain.user.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Careers implements Serializable {
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String title;
}
