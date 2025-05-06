package inha.gdgoc.domain.study.dto;

import inha.gdgoc.domain.study.enums.CreatorType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyDto {

    private Long id;
    private String title;

    private Long creatorId;
    private CreatorType creatorType;

    private String simpleIntroduce;
    private String activityIntroduce;

    private StudyStatus status;

    private LocalDateTime recruitStartDate;
    private LocalDateTime recruitEndDate;

    private LocalDateTime activityStartDate;
    private LocalDateTime activityEndDate;

    private String expectedTime;
    private String expectedPlace;
    private String imagePath;
}
