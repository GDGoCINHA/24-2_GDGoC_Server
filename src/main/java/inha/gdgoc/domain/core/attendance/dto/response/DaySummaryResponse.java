// DaySummaryResponse.java
package inha.gdgoc.domain.core.attendance.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DaySummaryResponse {
    private String date;
    private List<TeamSummary> perTeam;
    private long present;
    private long total;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamSummary {
        private String teamId;
        private String teamName;
        private long present;
        private long total;
    }
}