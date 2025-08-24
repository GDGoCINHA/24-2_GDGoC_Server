package inha.gdgoc.domain.study.dto.response;

import inha.gdgoc.domain.study.dto.MyStudyRecruitDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyStudyRecruitResponse {

    private List<MyStudyRecruitDto> recruiting;

    private List<MyStudyRecruitDto> recruited;
}
