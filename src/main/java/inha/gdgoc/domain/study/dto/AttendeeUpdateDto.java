package inha.gdgoc.domain.study.dto;

import inha.gdgoc.domain.study.enums.AttendeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendeeUpdateDto {

    private Long attendeeId;

    private AttendeeStatus status;

}
