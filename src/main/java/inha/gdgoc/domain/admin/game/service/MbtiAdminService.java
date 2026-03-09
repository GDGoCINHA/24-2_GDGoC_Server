package inha.gdgoc.domain.admin.game.service;

import inha.gdgoc.domain.admin.game.dto.request.MbtiTeamMatchRequest;
import inha.gdgoc.domain.admin.game.dto.response.MbtiAdminResultRowResponse;
import inha.gdgoc.domain.admin.game.dto.response.MbtiTeamMatchResponse;
import inha.gdgoc.domain.game.entity.MbtiResult;
import inha.gdgoc.domain.game.repository.MbtiResultRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MbtiAdminService {

    private static final String NO_RESULT_REASON = "NO_MBTI_RESULT";

    private final MbtiResultRepository mbtiResultRepository;

    @Transactional(readOnly = true)
    public Page<MbtiAdminResultRowResponse> searchResults(String keyword, Pageable pageable) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.isEmpty()) {
            return mbtiResultRepository.findAll(pageable).map(MbtiAdminResultRowResponse::from);
        }

        return mbtiResultRepository
                .findByNameContainingIgnoreCaseOrStudentIdContainingIgnoreCaseOrMbtiTypeContainingIgnoreCase(
                        query,
                        query,
                        query,
                        pageable
                )
                .map(MbtiAdminResultRowResponse::from);
    }

    @Transactional(readOnly = true)
    public MbtiTeamMatchResponse matchTeams(MbtiTeamMatchRequest request) {
        List<MbtiTeamMatchRequest.Candidate> rawCandidates = request.candidates();
        Map<String, MbtiTeamMatchRequest.Candidate> uniqueByStudentId = new LinkedHashMap<>();

        for (MbtiTeamMatchRequest.Candidate candidate : rawCandidates) {
            if (candidate == null) {
                continue;
            }

            String studentId = normalize(candidate.studentId());
            if (studentId.isEmpty()) {
                continue;
            }

            uniqueByStudentId.putIfAbsent(
                    studentId,
                    new MbtiTeamMatchRequest.Candidate(normalize(candidate.name()), studentId)
            );
        }

        List<MbtiTeamMatchRequest.Candidate> uniqueCandidates = new ArrayList<>(uniqueByStudentId.values());
        List<String> studentIds = uniqueCandidates.stream()
                .map(MbtiTeamMatchRequest.Candidate::studentId)
                .toList();

        Map<String, MbtiResult> resultMap = mbtiResultRepository.findByStudentIdIn(studentIds).stream()
                .collect(
                        LinkedHashMap::new,
                        (acc, row) -> acc.putIfAbsent(row.getStudentId(), row),
                        Map::putAll
                );

        List<MbtiTeamMatchResponse.Member> matchedMembers = new ArrayList<>();
        List<MbtiTeamMatchResponse.UnmatchedCandidate> unmatched = new ArrayList<>();

        for (MbtiTeamMatchRequest.Candidate candidate : uniqueCandidates) {
            MbtiResult matched = resultMap.get(candidate.studentId());
            if (matched == null) {
                unmatched.add(new MbtiTeamMatchResponse.UnmatchedCandidate(
                        candidate.name(),
                        candidate.studentId(),
                        NO_RESULT_REASON
                ));
                continue;
            }

            matchedMembers.add(new MbtiTeamMatchResponse.Member(
                    candidate.name().isEmpty() ? matched.getName() : candidate.name(),
                    candidate.studentId(),
                    matched.getMbtiType()
            ));
        }

        int teamSize = request.resolvedTeamSize();
        List<MbtiTeamMatchResponse.Team> teams = buildBalancedTeams(matchedMembers, teamSize);

        return new MbtiTeamMatchResponse(
                rawCandidates.size(),
                uniqueCandidates.size(),
                matchedMembers.size(),
                unmatched.size(),
                teamSize,
                teams.size(),
                teams,
                unmatched
        );
    }

    private List<MbtiTeamMatchResponse.Team> buildBalancedTeams(
            List<MbtiTeamMatchResponse.Member> members,
            int teamSize
    ) {
        if (members.isEmpty()) {
            return List.of();
        }

        int teamCount = (int) Math.ceil((double) members.size() / teamSize);
        List<TeamBucket> buckets = new ArrayList<>();
        for (int i = 0; i < teamCount; i += 1) {
            buckets.add(new TeamBucket(i + 1));
        }

        Map<String, List<MbtiTeamMatchResponse.Member>> grouped = members.stream()
                .filter(Objects::nonNull)
                .collect(
                        LinkedHashMap::new,
                        (acc, member) -> acc.computeIfAbsent(member.mbtiType(), key -> new ArrayList<>()).add(member),
                        Map::putAll
                );

        List<Deque<MbtiTeamMatchResponse.Member>> queues = grouped.values().stream()
                .sorted(Comparator.comparingInt((List<MbtiTeamMatchResponse.Member> list) -> list.size()).reversed())
                .map(list -> (Deque<MbtiTeamMatchResponse.Member>) new ArrayDeque<>(list))
                .toList();

        List<MbtiTeamMatchResponse.Member> ordered = interleaveByType(queues);

        for (MbtiTeamMatchResponse.Member member : ordered) {
            TeamBucket bucket = buckets.stream()
                    .min(
                            Comparator.comparingInt(TeamBucket::size)
                                    .thenComparingInt(team -> team.countType(member.mbtiType()))
                                    .thenComparingInt(TeamBucket::teamNumber)
                    )
                    .orElseThrow();

            bucket.add(member);
        }

        return buckets.stream()
                .map(TeamBucket::toResponse)
                .toList();
    }

    private List<MbtiTeamMatchResponse.Member> interleaveByType(
            Collection<Deque<MbtiTeamMatchResponse.Member>> queues
    ) {
        List<MbtiTeamMatchResponse.Member> result = new ArrayList<>();
        boolean hasRemaining = true;

        while (hasRemaining) {
            hasRemaining = false;
            for (Deque<MbtiTeamMatchResponse.Member> queue : queues) {
                MbtiTeamMatchResponse.Member member = queue.pollFirst();
                if (member == null) {
                    continue;
                }

                result.add(member);
                if (!queue.isEmpty()) {
                    hasRemaining = true;
                }
            }
        }

        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class TeamBucket {
        private final int teamNumber;
        private final List<MbtiTeamMatchResponse.Member> members = new ArrayList<>();
        private final Map<String, Integer> typeCounts = new HashMap<>();

        private TeamBucket(int teamNumber) {
            this.teamNumber = teamNumber;
        }

        private void add(MbtiTeamMatchResponse.Member member) {
            members.add(member);
            typeCounts.merge(member.mbtiType(), 1, Integer::sum);
        }

        private int size() {
            return members.size();
        }

        private int teamNumber() {
            return teamNumber;
        }

        private int countType(String mbtiType) {
            return typeCounts.getOrDefault(mbtiType, 0);
        }

        private MbtiTeamMatchResponse.Team toResponse() {
            return new MbtiTeamMatchResponse.Team(teamNumber, List.copyOf(members));
        }
    }
}
