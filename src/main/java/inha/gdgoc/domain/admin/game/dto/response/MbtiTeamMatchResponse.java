package inha.gdgoc.domain.admin.game.dto.response;

import java.util.List;

public record MbtiTeamMatchResponse(
        int totalCandidates,
        int uniqueCandidates,
        int matchedCount,
        int unmatchedCount,
        int teamSize,
        int teamCount,
        List<Team> teams,
        List<UnmatchedCandidate> unmatchedCandidates
) {
    public record Team(
            int teamNumber,
            List<Member> members
    ) {
    }

    public record Member(
            String name,
            String studentId,
            String mbtiType,
            boolean hasMbtiResult
    ) {
    }

    public record UnmatchedCandidate(
            String name,
            String studentId,
            String reason
    ) {
    }
}
