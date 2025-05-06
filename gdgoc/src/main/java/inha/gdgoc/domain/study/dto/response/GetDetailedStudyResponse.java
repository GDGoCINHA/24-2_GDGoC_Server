package inha.gdgoc.domain.study.dto.response;

import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.user.entity.User;
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
    public static GetDetailedStudyResponse from(Study study, User user) {
        return new GetDetailedStudyResponse(study.getId(), GetCreatorResponse.from(user), study.getTitle(),
                study.getSimpleIntroduce(), study.getActivityIntroduce(), study.getStatus(),
                study.getRecruitStartDate(), study.getRecruitEndDate(), study.getActivityStartDate(),
                study.getActivityEndDate(), study.getExpectedTime(), study.getExpectedPlace(), study.getImagePath());
    }
}
