package inha.gdgoc.domain.study.dto.request;

import inha.gdgoc.domain.study.dto.AttendeeUpdateDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class AttendeeUpdateRequest {

    List<AttendeeUpdateDto> attendees;
}
