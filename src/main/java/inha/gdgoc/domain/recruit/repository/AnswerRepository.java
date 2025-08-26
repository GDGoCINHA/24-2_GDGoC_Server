package inha.gdgoc.domain.recruit.repository;

import inha.gdgoc.domain.recruit.entity.Answer;
import inha.gdgoc.domain.recruit.entity.RecruitMember;
import inha.gdgoc.domain.recruit.enums.SurveyType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByRecruitMemberAndSurveyType(
            RecruitMember recruitMember,
            SurveyType surveyType
    );
}
