package inha.gdgoc.domain.admin.game.service;

import inha.gdgoc.domain.admin.game.dto.request.MbtiTeamMatchRequest;
import inha.gdgoc.domain.admin.game.dto.response.MbtiAdminResultRowResponse;
import inha.gdgoc.domain.admin.game.dto.response.MbtiTeamMatchResponse;
import inha.gdgoc.domain.game.entity.MbtiResult;
import inha.gdgoc.domain.game.repository.MbtiResultRepository;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
    private static final String EXCLUDED_PRIVILEGED_ROLE_REASON = "EXCLUDED_PRIVILEGED_ROLE";
    private static final Map<String, List<String>> TEAMMATE_COMPATIBILITY = Map.ofEntries(
            Map.entry("LPTI", List.of("CPTF", "CPUI", "LSTF")),
            Map.entry("LPTF", List.of("LSTF", "CPTF", "LPUI")),
            Map.entry("LSTI", List.of("LPTF", "CPUF", "LPUF")),
            Map.entry("LSTF", List.of("LPTI", "CPTF", "LPUF")),
            Map.entry("CPTI", List.of("LSTF", "LPTI", "LPUI")),
            Map.entry("CPTF", List.of("LPTI", "LSTF", "LSUI")),
            Map.entry("CSTI", List.of("CPUI", "LPTF", "LPUI")),
            Map.entry("CSTF", List.of("LSTF", "CPTF", "CPUF")),
            Map.entry("LPUI", List.of("CPTF", "LSTF", "CSUI")),
            Map.entry("LPUF", List.of("LSTI", "LSTF", "CPUF")),
            Map.entry("LSUI", List.of("LPTF", "CPTI", "LPUI")),
            Map.entry("LSUF", List.of("LPTF", "CPTF", "CPUF")),
            Map.entry("CPUI", List.of("LPTI", "LSTI", "CSTI")),
            Map.entry("CPUF", List.of("LSTI", "LPUF", "CPTF")),
            Map.entry("CSUI", List.of("LPUI", "LSTF", "CPTF")),
            Map.entry("CSUF", List.of("CPTF", "LSTF", "CPUF"))
    );

    private final MbtiResultRepository mbtiResultRepository;
    private final UserRepository userRepository;

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
        Map<String, MbtiTeamMatchRequest.Candidate> uniqueByCandidateKey = new LinkedHashMap<>();

        for (MbtiTeamMatchRequest.Candidate candidate : rawCandidates) {
            if (candidate == null) {
                continue;
            }

            String name = normalize(candidate.name());
            String studentYear = normalizeAcademicYear(candidate.studentId());
            if (name.isEmpty() || studentYear.isEmpty()) {
                continue;
            }

            String candidateKey = buildCandidateKey(name, studentYear);
            uniqueByCandidateKey.putIfAbsent(
                    candidateKey,
                    new MbtiTeamMatchRequest.Candidate(name, studentYear)
            );
        }

        List<MbtiTeamMatchRequest.Candidate> uniqueCandidates = new ArrayList<>(uniqueByCandidateKey.values());
        Map<String, UserRole> privilegedRoleByCandidateKey = userRepository
                .findByUserRoleIn(List.of(UserRole.LEAD, UserRole.ORGANIZER))
                .stream()
                .collect(
                        LinkedHashMap::new,
                        (acc, user) -> acc.putIfAbsent(
                                buildCandidateKey(user.getName(), user.getStudentId()),
                                user.getUserRole()
                        ),
                        Map::putAll
                );
        Map<String, MbtiResult> resultMap = mbtiResultRepository.findAll().stream()
                .collect(
                        LinkedHashMap::new,
                        (acc, row) -> acc.putIfAbsent(
                                buildCandidateKey(row.getName(), row.getStudentId()),
                                row
                        ),
                        Map::putAll
                );

        List<MbtiTeamMatchResponse.Member> matchedMembers = new ArrayList<>();
        List<MbtiTeamMatchResponse.Member> unmatchedMembers = new ArrayList<>();
        List<MbtiTeamMatchResponse.UnmatchedCandidate> unmatched = new ArrayList<>();

        for (MbtiTeamMatchRequest.Candidate candidate : uniqueCandidates) {
            String candidateKey = buildCandidateKey(candidate.name(), candidate.studentId());
            UserRole userRole = privilegedRoleByCandidateKey.get(candidateKey);
            if (userRole == UserRole.LEAD || userRole == UserRole.ORGANIZER) {
                unmatched.add(new MbtiTeamMatchResponse.UnmatchedCandidate(
                        candidate.name(),
                        candidate.studentId(),
                        EXCLUDED_PRIVILEGED_ROLE_REASON
                ));
                continue;
            }

            MbtiResult matched = resultMap.get(candidateKey);
            if (matched == null) {
                unmatchedMembers.add(new MbtiTeamMatchResponse.Member(
                        candidate.name(),
                        candidate.studentId(),
                        null,
                        false
                ));
                continue;
            }

            matchedMembers.add(new MbtiTeamMatchResponse.Member(
                    candidate.name().isEmpty() ? matched.getName() : candidate.name(),
                    candidate.studentId(),
                    matched.getMbtiType(),
                    true
            ));
        }

        int teamSize = request.resolvedTeamSize();
        TeamBuildResult buildResult = buildBalancedTeams(matchedMembers, unmatchedMembers, teamSize);

        return new MbtiTeamMatchResponse(
                rawCandidates.size(),
                uniqueCandidates.size(),
                buildResult.assignedCount(),
                unmatched.size(),
                teamSize,
                buildResult.teams().size(),
                buildResult.teams(),
                unmatched
        );
    }

    private TeamBuildResult buildBalancedTeams(
            List<MbtiTeamMatchResponse.Member> matchedMembers,
            List<MbtiTeamMatchResponse.Member> unmatchedMembers,
            int teamSize
    ) {
        int totalMembers = matchedMembers.size() + unmatchedMembers.size();
        if (totalMembers == 0) {
            return new TeamBuildResult(List.of());
        }

        int teamCount = (int) Math.ceil((double) totalMembers / teamSize);

        List<TeamBucket> buckets = new ArrayList<>();
        for (int i = 0; i < teamCount; i += 1) {
            int baseSize = totalMembers / teamCount + (i < totalMembers % teamCount ? 1 : 0);
            int unmatchedTarget = unmatchedMembers.size() / teamCount + (i < unmatchedMembers.size() % teamCount ? 1 : 0);
            unmatchedTarget = Math.min(unmatchedTarget, baseSize);
            buckets.add(new TeamBucket(i + 1, unmatchedTarget, baseSize - unmatchedTarget));
        }

        for (MbtiTeamMatchResponse.Member member : unmatchedMembers) {
            TeamBucket bucket = buckets.stream()
                    .filter(TeamBucket::canAcceptUnmatched)
                    .min(Comparator.comparingInt(TeamBucket::size).thenComparingInt(TeamBucket::teamNumber))
                    .orElseThrow();

            bucket.addUnmatched(member);
        }

        List<MbtiTeamMatchResponse.Member> orderedMatched = buildCompatibilitySeedOrder(matchedMembers);

        for (MbtiTeamMatchResponse.Member member : orderedMatched) {
            TeamBucket bucket = buckets.stream()
                    .filter(TeamBucket::canAcceptMatched)
                    .max(
                            Comparator.comparingInt((TeamBucket team) -> team.compatibilityScoreFor(member))
                                    .thenComparing(Comparator.comparingInt(TeamBucket::size).reversed())
                                    .thenComparing(Comparator.comparingInt(TeamBucket::teamNumber).reversed())
                    )
                    .orElseThrow();

            bucket.addMatched(member);
        }

        List<MbtiTeamMatchResponse.Team> teams = buckets.stream().map(TeamBucket::toResponse).toList();
        return new TeamBuildResult(teams);
    }

    private List<MbtiTeamMatchResponse.Member> buildCompatibilitySeedOrder(
            List<MbtiTeamMatchResponse.Member> members
    ) {
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

        List<MbtiTeamMatchResponse.Member> ordered = new ArrayList<>();
        boolean hasRemaining = true;

        while (hasRemaining) {
            hasRemaining = false;
            for (Deque<MbtiTeamMatchResponse.Member> queue : queues) {
                MbtiTeamMatchResponse.Member member = queue.pollFirst();
                if (member == null) {
                    continue;
                }

                ordered.add(member);
                if (!queue.isEmpty()) {
                    hasRemaining = true;
                }
            }
        }

        return ordered;
    }

    private static int pairCompatibilityScore(String sourceType, String targetType) {
        if (sourceType == null || targetType == null) {
            return 0;
        }

        List<String> sourceMatches = TEAMMATE_COMPATIBILITY.getOrDefault(sourceType, List.of());
        List<String> targetMatches = TEAMMATE_COMPATIBILITY.getOrDefault(targetType, List.of());

        boolean sourcePrefersTarget = sourceMatches.contains(targetType);
        boolean targetPrefersSource = targetMatches.contains(sourceType);

        if (sourcePrefersTarget && targetPrefersSource) {
            return 3;
        }
        if (sourcePrefersTarget || targetPrefersSource) {
            return 1;
        }
        return 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildCandidateKey(String name, String studentIdOrYear) {
        String normalizedName = normalize(name).toLowerCase();
        String normalizedYear = normalizeAcademicYear(studentIdOrYear);
        if (normalizedName.isEmpty() || normalizedYear.isEmpty()) {
            return "";
        }
        return normalizedName + "|" + normalizedYear;
    }

    private String normalizeAcademicYear(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() == 2) {
            return digits;
        }
        if (digits.startsWith("12") && digits.length() >= 4) {
            return digits.substring(2, 4);
        }
        if (digits.length() >= 2) {
            return digits.substring(0, 2);
        }
        return "";
    }

    private static final class TeamBucket {
        private final int teamNumber;
        private final int unmatchedTarget;
        private final int matchedTarget;
        private final List<MbtiTeamMatchResponse.Member> members = new ArrayList<>();
        private int unmatchedCount;

        private TeamBucket(int teamNumber, int unmatchedTarget, int matchedTarget) {
            this.teamNumber = teamNumber;
            this.unmatchedTarget = unmatchedTarget;
            this.matchedTarget = matchedTarget;
        }

        private void addMatched(MbtiTeamMatchResponse.Member member) {
            members.add(member);
        }

        private void addUnmatched(MbtiTeamMatchResponse.Member member) {
            members.add(member);
            unmatchedCount += 1;
        }

        private int size() {
            return members.size();
        }

        private int teamNumber() {
            return teamNumber;
        }

        private boolean canAcceptUnmatched() {
            return unmatchedCount < unmatchedTarget && size() < unmatchedTarget + matchedTarget;
        }

        private boolean canAcceptMatched() {
            return matchedCount() < matchedTarget && size() < unmatchedTarget + matchedTarget;
        }

        private int matchedCount() {
            return size() - unmatchedCount;
        }

        private int compatibilityScoreFor(MbtiTeamMatchResponse.Member candidate) {
            return members.stream()
                    .filter(MbtiTeamMatchResponse.Member::hasMbtiResult)
                    .mapToInt(member -> pairCompatibilityScore(candidate.mbtiType(), member.mbtiType()))
                    .sum();
        }

        private MbtiTeamMatchResponse.Team toResponse() {
            return new MbtiTeamMatchResponse.Team(teamNumber, List.copyOf(members));
        }
    }

    private record TeamBuildResult(List<MbtiTeamMatchResponse.Team> teams) {
        private int assignedCount() {
            return teams.stream().mapToInt(team -> team.members().size()).sum();
        }
    }
}
