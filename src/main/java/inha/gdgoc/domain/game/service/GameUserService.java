package inha.gdgoc.domain.game.service;

import inha.gdgoc.domain.game.dto.request.GameUserRequest;
import inha.gdgoc.domain.game.dto.response.GameUserResponse;
import inha.gdgoc.domain.game.entity.GameUser;
import inha.gdgoc.domain.game.repository.GameUserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GameUserService {

    private final GameUserRepository gameUserRepository;

    @Transactional
    public List<GameUserResponse> saveGameResultAndGetRanking(GameUserRequest gameUserRequest) {
        // 유저 정보 저장
        GameUser gameUser = gameUserRequest.toEntity();
        gameUserRepository.save(gameUser);

        return findUserRankings();
    }

    public List<GameUserResponse> findUserRankings() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime startOfDay = today.atStartOfDay(); // 00:00:00
        LocalDateTime endOfDay = today.atTime(23, 59, 59); // 23:59:59

        // 전체 유저 순위 리스트 가져오기
        List<GameUser> results = gameUserRepository.findAllByCreatedAtBetweenOrderByTypingSpeedAsc(startOfDay,
                endOfDay);

        return results.stream()
                .map(user -> new GameUserResponse(results.indexOf(user) + 1, user))
                .collect(Collectors.toList());
    }

}
