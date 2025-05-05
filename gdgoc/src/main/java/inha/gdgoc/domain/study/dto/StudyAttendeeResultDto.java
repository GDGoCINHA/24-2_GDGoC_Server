package inha.gdgoc.domain.study.dto;

import inha.gdgoc.domain.study.enums.AttendeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyAttendeeResultDto {

    private Long studyId;

    private String title;

    private LocalDateTime recruitEndDate;

    private AttendeeStatus status;
}
