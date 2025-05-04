package inha.gdgoc.domain.study.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetStudyAttendeeResponse {
    private String name;

    private String phone;

    private String major;

    private String studentId;

    private String introduce;

    private String activityTime;

}
