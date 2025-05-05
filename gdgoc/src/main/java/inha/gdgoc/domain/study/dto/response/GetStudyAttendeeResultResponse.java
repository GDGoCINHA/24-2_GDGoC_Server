package inha.gdgoc.domain.study.dto.response;

import inha.gdgoc.domain.study.dto.StudyAttendeeResultDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetStudyAttendeeResultResponse {

    private List<StudyAttendeeResultDto> recruiting;
}
