package inha.gdgoc.domain.game.service;

import inha.gdgoc.domain.game.dto.request.Rythm8beatScoreRequest;
import inha.gdgoc.domain.game.dto.response.Rythm8beatRankItemResponse;
import inha.gdgoc.domain.game.dto.response.Rythm8beatRankingResponse;
import inha.gdgoc.domain.game.entity.Rythm8beatScore;
import inha.gdgoc.domain.game.repository.Rythm8beatScoreRepository;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class Rythm8beatScoreService {

    private final Rythm8beatScoreRepository rythm8beatScoreRepository;

    public void submitScore(Rythm8beatScoreRequest request) {
        rythm8beatScoreRepository.findByPhoneNumber(request.getPhoneNumber())
                .ifPresentOrElse(
                        entity -> entity.updateIfHigherScore(
                                request.getNickname(),
                                request.getScore(),
                                request.getStageReached() != null ? request.getStageReached() : 1
                        ),
                        () -> rythm8beatScoreRepository.save(Rythm8beatScore.builder()
                                .phoneNumber(request.getPhoneNumber())
                                .nickname(request.getNickname())
                                .score(request.getScore())
                                .stageReached(request.getStageReached() != null ? request.getStageReached() : 1)
                                .build())
                );
    }

    @Transactional(readOnly = true)
    public Rythm8beatRankingResponse getRanking(String phoneNumber) {
        List<Rythm8beatScore> top3 = rythm8beatScoreRepository.findTop3ByOrderByScoreDescUpdatedAtAsc();

        List<Rythm8beatRankItemResponse> top3Response = IntStream.range(0, top3.size())
                .mapToObj(i -> new Rythm8beatRankItemResponse(i + 1, top3.get(i).getNickname(), top3.get(i).getScore()))
                .toList();

        Rythm8beatRankItemResponse userRank = null;
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            userRank = rythm8beatScoreRepository.findByPhoneNumber(phoneNumber)
                    .map(gs -> {
                        long rank = rythm8beatScoreRepository.countByScoreGreaterThan(gs.getScore()) + 1;
                        return new Rythm8beatRankItemResponse((int) rank, gs.getNickname(), gs.getScore());
                    })
                    .orElse(null);
        }

        return new Rythm8beatRankingResponse(top3Response, userRank);
    }

    public void resetAll() {
        rythm8beatScoreRepository.deleteAll();
    }
}
