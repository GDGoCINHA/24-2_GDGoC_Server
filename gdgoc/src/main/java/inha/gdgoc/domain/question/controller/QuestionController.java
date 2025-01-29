package inha.gdgoc.domain.question.controller;

import inha.gdgoc.domain.question.dto.QuestionRequest;
import inha.gdgoc.domain.question.entity.Question;
import inha.gdgoc.domain.question.service.QuestionService;
import inha.gdgoc.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
@RequiredArgsConstructor
@Controller
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping("/question")
    public ApiResponse<Long> createQuestion(@RequestBody QuestionRequest questionRequest) {
        Question savedQuestion = questionService.save(questionRequest);

        return ApiResponse.success(savedQuestion.getId(), "질문이 성공적으로 생성되었습니다.");
    }

}
