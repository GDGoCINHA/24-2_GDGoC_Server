package inha.gdgoc.domain.question.repository;

import inha.gdgoc.domain.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
