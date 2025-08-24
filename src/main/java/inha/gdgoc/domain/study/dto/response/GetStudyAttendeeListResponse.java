package inha.gdgoc.domain.study.dto.response;

import inha.gdgoc.domain.study.dto.StudyAttendeeDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetStudyAttendeeListResponse {
    private List<StudyAttendeeDto> attendees;
}
