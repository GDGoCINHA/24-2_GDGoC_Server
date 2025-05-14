package inha.gdgoc.domain.study.dto.response;

import inha.gdgoc.domain.study.enums.StudyStatus;
import java.time.LocalDateTime;

public record GetDetailedStudyResponse(Long id,
                                       GetCreatorResponse creator,
                                       String title,
                                       String simpleIntroduce,
                                       String activityIntroduce,
                                       StudyStatus status,
                                       LocalDateTime recruitStartDate,
                                       LocalDateTime recruitEndDate,
                                       LocalDateTime activityStartDate,
                                       LocalDateTime activityEndDate,
                                       String expectedTime,
                                       String expectedPlace,
                                       String imagePath
) {

}
