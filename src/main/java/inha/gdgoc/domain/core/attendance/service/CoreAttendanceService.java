package inha.gdgoc.domain.core.attendance.service;

import inha.gdgoc.domain.core.attendance.dto.response.DaySummaryResponse;
import inha.gdgoc.domain.core.attendance.dto.response.MemberResponse;
import inha.gdgoc.domain.core.attendance.dto.response.TeamResponse;
import inha.gdgoc.domain.core.attendance.entity.Meeting;
import inha.gdgoc.domain.core.attendance.repository.AttendanceRecordRepository;
import inha.gdgoc.domain.core.attendance.repository.MeetingRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoreAttendanceService {

    private final MeetingRepository meetingRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;

    /* ===================== Meetings (dates) ===================== */

    @Transactional(readOnly = true)
    public List<String> getDates() {
        return meetingRepository.findAll(Sort.by(Sort.Direction.DESC, "meetingDate"))
                .stream()
                .map(m -> m.getMeetingDate().toString())
                .toList();
    }

    @Transactional
    public void addDate(String date) {
        LocalDate d = LocalDate.parse(date);
        meetingRepository.findByMeetingDate(d)
                .orElseGet(() -> meetingRepository.save(Meeting.builder().meetingDate(d).build()));
    }

    @Transactional
    public void deleteDate(String date) {
        LocalDate d = LocalDate.parse(date);
        meetingRepository.findByMeetingDate(d).ifPresent(m -> {
            // FK ON DELETE CASCADE라면 meeting만 지우면 attendance도 함께 삭제됨
            meetingRepository.deleteById(m.getId());
        });
    }

    /* ===================== Teams ===================== */

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsForLead(TeamType leadTeam) {
        var roles = List.of(UserRole.CORE, UserRole.LEAD);
        List<User> users = userRepository.findByTeamAndUserRoleIn(leadTeam, roles);
        return toTeamResponsesGrouped(users);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsForOrganizerOrAdmin() {
        var roles = List.of(UserRole.CORE, UserRole.LEAD);
        List<User> users = userRepository.findByUserRoleIn(roles);
        return toTeamResponsesGrouped(users);
    }

    private List<TeamResponse> toTeamResponsesGrouped(List<User> users) {
        Map<TeamType, List<User>> grouped = users.stream()
                .filter(u -> u.getTeam() != null)
                .collect(Collectors.groupingBy(User::getTeam, LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(e -> {
                    TeamType team = e.getKey();
                    List<MemberResponse> members = e.getValue().stream()
                            .sorted(Comparator.comparing(User::getName))
                            .map(u -> new MemberResponse(String.valueOf(u.getId()), u.getName()))
                            .toList();
                    return new TeamResponse(team.name(), team.getLabel(), members);
                })
                .toList();
    }

    /* ===================== Attendance ===================== */

    public record UserIdValidationResult(List<Long> validIds, List<Long> invalidIds) {}

    /** 주어진 userIds 중 team 소속만 골라냄 */
    @Transactional(readOnly = true)
    public UserIdValidationResult filterUserIdsNotInTeam(TeamType team, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new UserIdValidationResult(List.of(), List.of());
        }
        var users = userRepository.findAllById(userIds);
        var inTeam = new HashSet<Long>();
        for (var u : users) {
            if (u.getTeam() == team) inTeam.add(u.getId());
        }
        List<Long> valid = userIds.stream().filter(inTeam::contains).toList();
        List<Long> invalid = userIds.stream().filter(id -> !inTeam.contains(id)).toList();
        return new UserIdValidationResult(valid, invalid);
    }

    /** 배치로 출석 true/false 반영 (고성능 UPSERT) */
    @Transactional
    public long setAttendance(String date, List<Long> userIds, boolean present) {
        if (userIds == null || userIds.isEmpty()) return 0L;

        LocalDate d = LocalDate.parse(date);
        Long meetingId = ensureMeetingAndGetId(d);

        Long[] arr = userIds.toArray(Long[]::new); // ✅ List -> Array
        int affected = attendanceRecordRepository
                .upsertBatchByMeetingId(meetingId, arr, present);
        return Math.max(affected, 0);
    }

    /** 특정 날짜에 대해 팀원 + 현재 출석 여부 목록 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMembersWithPresence(String date, TeamType teamOrNull) {
        LocalDate d = LocalDate.parse(date);
        Map<Long, Boolean> day = getPresenceMap(d);

        var roles = List.of(UserRole.CORE, UserRole.LEAD);
        List<User> users = (teamOrNull == null)
                ? userRepository.findByUserRoleIn(roles)
                : userRepository.findByTeamAndUserRoleIn(teamOrNull, roles);

        return users.stream()
                .filter(u -> u.getTeam() != null)
                .sorted(Comparator.comparing(User::getName))
                .map(u -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("userId", String.valueOf(u.getId()));
                    row.put("name", u.getName());
                    row.put("team", u.getTeam().getLabel());
                    row.put("present", day.getOrDefault(u.getId(), false));
                    row.put("lastModifiedAt", null); // 추후 updatedAt/updatedBy 확장 시 채우기
                    return row;
                })
                .toList();
    }

    /** userIds 로부터 단일 팀을 추론 (다르면 empty) */
    @Transactional(readOnly = true)
    public Optional<TeamType> inferTeamFromUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Optional.empty();
        var users = userRepository.findAllById(userIds).stream()
                .filter(u -> u.getTeam() != null)
                .toList();
        if (users.isEmpty()) return Optional.empty();

        TeamType first = users.get(0).getTeam();
        boolean same = users.stream().allMatch(u -> first == u.getTeam());
        return same ? Optional.of(first) : Optional.empty();
    }

    /* ===================== Summary ===================== */

    @Transactional(readOnly = true)
    public DaySummaryResponse summary(String date, TeamType teamForLeadOrNull) {
        LocalDate d = LocalDate.parse(date);
        Map<Long, Boolean> day = getPresenceMap(d);

        var roles = List.of(UserRole.CORE, UserRole.LEAD);
        List<User> baseUsers = (teamForLeadOrNull == null)
                ? userRepository.findByUserRoleIn(roles)
                : userRepository.findByTeamAndUserRoleIn(teamForLeadOrNull, roles);

        Map<TeamType, List<User>> byTeam = baseUsers.stream()
                .filter(u -> u.getTeam() != null)
                .collect(Collectors.groupingBy(User::getTeam, LinkedHashMap::new, Collectors.toList()));

        var perTeam = byTeam.entrySet().stream()
                .map(e -> {
                    TeamType team = e.getKey();
                    List<User> us = e.getValue();
                    long p = us.stream().filter(u -> day.getOrDefault(u.getId(), false)).count();
                    return new DaySummaryResponse.TeamSummary(team.name(), team.getLabel(), p, us.size());
                })
                .sorted(Comparator.comparing(DaySummaryResponse.TeamSummary::getTeamName))
                .toList();

        long present = perTeam.stream().mapToLong(DaySummaryResponse.TeamSummary::getPresent).sum();
        long total = perTeam.stream().mapToLong(DaySummaryResponse.TeamSummary::getTotal).sum();

        return new DaySummaryResponse(date, perTeam, present, total);
    }

    /** 요약 CSV 생성 (UTF-8) */
    @Transactional(readOnly = true)
    public String buildSummaryCsv(String date, TeamType teamOrNull) {
        DaySummaryResponse s = summary(date, teamOrNull);
        StringBuilder sb = new StringBuilder();
        sb.append("date,team_id,team_name,present,total\n");
        for (var t : s.getPerTeam()) {
            sb.append(escape(date)).append(',')
                    .append(escape(t.getTeamId())).append(',')
                    .append(escape(t.getTeamName())).append(',')
                    .append(t.getPresent()).append(',')
                    .append(t.getTotal()).append('\n');
        }
        sb.append(escape(date)).append(',')
                .append("ALL").append(',')
                .append("전체").append(',')
                .append(s.getPresent()).append(',')
                .append(s.getTotal()).append('\n');

        return new String(sb.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    /* ===================== helpers ===================== */

    /** date로 meeting을 보장하고 meetingId 반환 */
    @Transactional
    protected Long ensureMeetingAndGetId(LocalDate date) {
        return meetingRepository.findByMeetingDate(date)
                .map(Meeting::getId)
                .orElseGet(() -> meetingRepository.save(
                        Meeting.builder().meetingDate(date).build()
                ).getId());
    }

    /** 특정 날짜의 출석 맵(userId → present) */
    @Transactional(readOnly = true)
    protected Map<Long, Boolean> getPresenceMap(LocalDate date) {
        Map<Long, Boolean> map = new HashMap<>();
        var meetingOpt = meetingRepository.findByMeetingDate(date);
        if (meetingOpt.isEmpty()) return map;

        Long meetingId = meetingOpt.get().getId();
        attendanceRecordRepository.findPresencePairsByMeetingId(meetingId).forEach(row -> {
            Long uid = ((Number) row[0]).longValue();
            Boolean present = (Boolean) row[1];
            map.put(uid, present != null && present);
        });
        return map;
    }

    private static String escape(String s) {
        if (s == null) return "";
        String needsQuote = ",\"\n";
        boolean mustQuote = s.chars().anyMatch(ch -> needsQuote.indexOf(ch) >= 0);
        if (!mustQuote) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}