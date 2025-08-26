package inha.gdgoc.domain.recruit.repository;

import inha.gdgoc.domain.recruit.entity.Answer;
import inha.gdgoc.domain.recruit.entity.RecruitMember;
import inha.gdgoc.domain.recruit.enums.SurveyType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /**
     * 주어진 모집 멤버와 설문 타입에 해당하는 모든 Answer 엔티티를 조회합니다.
     *
     * @param recruitMember 조회할 Answer의 recruitMember 필터
     * @param surveyType    조회할 Answer의 surveyType 필터
     * @return 해당 조건을 만족하는 Answer 목록(없으면 빈 리스트)
     */
    List<Answer> findByRecruitMemberAndSurveyType(
            RecruitMember recruitMember,
            SurveyType surveyType
    );
}
