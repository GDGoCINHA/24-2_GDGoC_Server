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

    /**
     * 플레이어의 점수를 제출하고, 동일 전화번호의 기존 기록이 있으면 더 높은 점수로만 갱신하며 없으면 새 기록을 저장한다.
     *
     * @param request 제출할 점수 정보. phoneNumber, nickname, score 필드를 사용하며 stageReached가 null이면 1로 간주한다.
     */
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

    /**
     * 상위 3명의 점수와 요청된 사용자의 랭킹 정보를 제공한다.
     *
     * @param phoneNumber 조회할 사용자의 전화번호. null 또는 공백이면 사용자 랭킹 항목은 포함되지 않는다.
     * @return 상위 3명 항목 리스트와 요청 사용자의 랭킹 항목(존재하지 않으면 null)을 포함한 랭킹 응답 객체.
     */
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

    /**
     * 저장된 모든 리듬8비트 점수 엔티티를 삭제합니다.
     */
    public void resetAll() {
        rythm8beatScoreRepository.deleteAll();
    }
}
