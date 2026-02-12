package inha.gdgoc.domain.recruit.member.repository;

import inha.gdgoc.domain.recruit.member.entity.Answer;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.SurveyType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByRecruitMemberAndSurveyType(
            RecruitMember recruitMember,
            SurveyType surveyType
    );
}
