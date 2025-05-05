package inha.gdgoc.domain.study.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyStudyRecruitDto {

    private Long id;

    private String title;

    private LocalDateTime activityStartDate;

    private LocalDateTime activityEndDate;

}
