package inha.gdgoc.domain.question.repository;

import inha.gdgoc.domain.question.entity.Question;
import inha.gdgoc.domain.question.enums.SurveyType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findAllBy(SurveyType surveyType);
}
