package inha.gdgoc.domain.study.dto.request;

import inha.gdgoc.domain.study.enums.CreatorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyCreateRequest {

    private String title;

    private String simpleIntroduce;

    private String activityIntroduce;

    private CreatorType creatorType;

    private LocalDateTime recruitStartDate;

    private LocalDateTime recruitEndDate;

    private LocalDateTime activityStartDate;

    private LocalDateTime activityEndDate;

    private String expectedTime;

    private String expectedPlace;

    private String imagePath;
}
