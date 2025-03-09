package inha.gdgoc.domain.game.service;

import inha.gdgoc.domain.game.dto.request.GameQuestionRequest;
import inha.gdgoc.domain.game.dto.response.GameQuestionResponse;
import inha.gdgoc.domain.game.entity.GameQuestion;
import inha.gdgoc.domain.game.repository.GameQuestionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GameQuestionService {

    private final List<String> LANGUAGES = List.of("python", "javascript", "cpp", "java", "assembly");
    private final Random random = new Random();

    private final GameQuestionRepository gameQuestionRepository;

    public void saveQuestion(GameQuestionRequest request) {
        gameQuestionRepository.save(request.toEntity());
    }

    public List<GameQuestionResponse> getRandomQuestionsByLanguage() {
        List<GameQuestionResponse> selectedQuestions = new ArrayList<>();

        for (String language : LANGUAGES) {
            List<GameQuestion> questions = gameQuestionRepository.findByLanguage(language);
            if (!questions.isEmpty()) {
                int index = random.nextInt(questions.size());
                selectedQuestions.add(new GameQuestionResponse(questions.get(index)));
            }
        }

        return selectedQuestions;
    }
}
