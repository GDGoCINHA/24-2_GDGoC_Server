package inha.gdgoc.domain.study.dto.response;

import inha.gdgoc.domain.study.dto.StudyDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudyListRequest {

    private List<StudyDto> studyList;
}
