package inha.gdgoc.domain.question.service;

import inha.gdgoc.domain.question.dto.QuestionRequestDto;
import inha.gdgoc.domain.question.entity.Question;
import inha.gdgoc.domain.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;

    public Question save(QuestionRequestDto questionRequestDto) {
        return questionRepository.save(questionRequestDto.toEntity());
    }
}
