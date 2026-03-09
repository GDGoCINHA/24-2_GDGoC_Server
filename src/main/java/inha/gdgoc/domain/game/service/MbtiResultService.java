package inha.gdgoc.domain.game.service;

import inha.gdgoc.domain.game.dto.request.MbtiResultRequest;
import inha.gdgoc.domain.game.dto.response.MbtiResultResponse;
import inha.gdgoc.domain.game.dto.response.MbtiStatsResponse;
import inha.gdgoc.domain.game.dto.response.MbtiTypeCountResponse;
import inha.gdgoc.domain.game.entity.MbtiResult;
import inha.gdgoc.domain.game.repository.MbtiResultRepository;
import java.util.Comparator;
import java.util.List;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MbtiResultService {

    private final MbtiResultRepository mbtiResultRepository;

    @Transactional
    public MbtiResultResponse upsertMbtiResult(MbtiResultRequest request) {
        MbtiResult result = mbtiResultRepository.findByNameAndStudentId(request.getName(), request.getStudentId())
                .map(existing -> {
                    existing.updateMbtiType(request.getMbtiType());
                    return existing;
                })
                .orElseGet(() -> mbtiResultRepository.save(request.toEntity()));

        return new MbtiResultResponse(result);
    }

    public MbtiStatsResponse getMbtiStats() {
        long totalCount = mbtiResultRepository.count();
        List<MbtiTypeCountResponse> typeCounts = mbtiResultRepository.countByMbtiType().stream()
                .map(item -> new MbtiTypeCountResponse(item.getMbtiType(), item.getCount()))
                .sorted(Comparator.comparingLong(MbtiTypeCountResponse::getCount).reversed()
                        .thenComparing(MbtiTypeCountResponse::getMbtiType))
                .toList();

        return new MbtiStatsResponse(totalCount, typeCounts);
    }
}
