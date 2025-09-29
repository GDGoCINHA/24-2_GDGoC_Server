// TeamResponse.java
package inha.gdgoc.domain.core.attendance.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private String id;
    private String name;
    private String lead;
    private List<MemberResponse> members;
}