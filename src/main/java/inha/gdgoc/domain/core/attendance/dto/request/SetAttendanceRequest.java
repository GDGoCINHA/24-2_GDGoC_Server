// SetAttendanceRequest.java
package inha.gdgoc.domain.core.attendance.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SetAttendanceRequest {
    private boolean present;
}