// DateListResponse.java
package inha.gdgoc.domain.core.attendance.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DateListResponse {
    private List<String> dates;
}