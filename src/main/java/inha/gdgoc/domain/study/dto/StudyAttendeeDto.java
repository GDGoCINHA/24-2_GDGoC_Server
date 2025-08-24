package inha.gdgoc.domain.study.dto;

import inha.gdgoc.domain.study.enums.AttendeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyAttendeeDto {
    private Long id;

    private String name;

    private String major;

    private String studentId;

    private AttendeeStatus status;
}
