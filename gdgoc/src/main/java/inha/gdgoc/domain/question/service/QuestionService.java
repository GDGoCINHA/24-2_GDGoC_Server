package inha.gdgoc.domain.question.service;

import inha.gdgoc.domain.question.dto.QuestionRequest;
import inha.gdgoc.domain.question.entity.Question;
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
}
