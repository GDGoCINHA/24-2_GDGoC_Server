package inha.gdgoc.domain.question.service;

import inha.gdgoc.domain.question.dto.request.QuestionRequest;
import inha.gdgoc.domain.question.dto.response.QuestionResponses;
import inha.gdgoc.domain.question.entity.Question;
import inha.gdgoc.domain.question.enums.SurveyType;
import inha.gdgoc.domain.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;

    public Question create(QuestionRequest questionRequest) {
        return questionRepository.save(questionRequest.toEntity());
    }

    public QuestionResponses readAllQuestionsBySurveyType(SurveyType surveyType) {
        /*
            List<Question> questions = questionRepository.findAllBySurveyType(surveyType);
            sortByOrderAscending(questions);
            return QuestionResponses~..
         */
    }
}
