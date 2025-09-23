// inha/gdgoc/domain/core/attendance/service/CoreAttendanceService.java
package inha.gdgoc.domain.core.attendance.service;

import inha.gdgoc.domain.core.attendance.dto.response.DaySummaryResponse;
import inha.gdgoc.domain.core.attendance.dto.response.MemberResponse;
import inha.gdgoc.domain.core.attendance.dto.response.TeamResponse;
import inha.gdgoc.domain.core.attendance.entity.Member;
import inha.gdgoc.domain.core.attendance.entity.Team;
import inha.gdgoc.domain.core.attendance.repository.AttendanceRecordRepository;
import inha.gdgoc.domain.core.attendance.repository.MemberRepository;
import inha.gdgoc.domain.core.attendance.repository.TeamRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoreAttendanceService {

    private final TeamRepository teamRepo;
    private final MemberRepository memberRepo;
    private final AttendanceRecordRepository recordRepo;

    // 날짜 목록만 내부 보관 (임시 운용)
    private final LinkedHashSet<String> dates = new LinkedHashSet<>();

    private static String uuid(String p) {
        return p + "_" + java.util.UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
    }

    private static String dkey(String date) {return date.replaceAll("-", "");}

    @PostConstruct
    void initSeed() { // ✅ 빈 생성 후 시드
        seed();
    }

    private void seed() {
        String today = LocalDate.now().toString();
        dates.add(today);

        Team alpha = new Team(uuid("team"), "Alpha", "김정민");
        alpha.getMembers().add(new Member(uuid("m"), "홍길동"));
        alpha.getMembers().add(new Member(uuid("m"), "이서연"));
        teamRepo.save(alpha);

        Team beta = new Team(uuid("team"), "Beta", "이나경");
        beta.getMembers().add(new Member(uuid("m"), "장우진"));
        beta.getMembers().add(new Member(uuid("m"), "유하늘"));
        teamRepo.save(beta);
    }

    /* Dates */
    public List<String> getDates() {return new ArrayList<>(dates);}

    public void addDate(String date) {dates.add(date);}

    public void deleteDate(String date) {
        dates.remove(date);
        recordRepo.removeDate(dkey(date));
    }

    /* Teams */
    public List<TeamResponse> getTeams(String leadName, String teamId) {
        return teamRepo.findAll()
                .stream()
                .filter(t -> leadName == null || leadName.isBlank() || t.getLead().equals(leadName))
                .filter(t -> teamId == null || teamId.isBlank() || t.getId().equals(teamId))
                .map(this::toTeamResponse)
                .collect(Collectors.toList());
    }

    /* Members */
    public void addMember(String teamId, String name) {
        Team t = team(teamId);
        memberRepo.add(t, new Member(uuid("m"), name));
        teamRepo.save(t);
    }

    public void renameMember(String teamId, String memberId, String name) {
        Team t = team(teamId);
        Member m = memberRepo.find(t, memberId).orElseThrow(() -> new NoSuchElementException("member not found"));
        m.setName(name);
        teamRepo.save(t);
    }

    public void removeMember(String teamId, String memberId) {
        Team t = team(teamId);
        memberRepo.remove(t, memberId);
        teamRepo.save(t);
        recordRepo.removeMemberEverywhere(teamId, memberId);
    }

    /* Attendance */
    public void setAttendance(String date, String teamId, String memberId, boolean present) {
        recordRepo.setPresence(dkey(date), teamId, memberId, present);
    }

    public long setAll(String date, String teamId, boolean present) {
        Team t = team(teamId);
        List<String> memberIds = t.getMembers().stream().map(Member::getId).toList();
        return recordRepo.setAll(dkey(date), teamId, memberIds, present);
    }

    /* Summary */
    public DaySummaryResponse summary(String date, String leadName, String teamId) {
        var dm = recordRepo.getDay(dkey(date));
        var per = teamRepo.findAll()
                .stream()
                .filter(t -> leadName == null || leadName.isBlank() || t.getLead().equals(leadName))
                .filter(t -> teamId == null || teamId.isBlank() || t.getId().equals(teamId))
                .map(t -> {
                    var tm = dm.getOrDefault(t.getId(), Map.of());
                    long p = t.getMembers().stream().filter(m -> tm.getOrDefault(m.getId(), false)).count();
                    return new DaySummaryResponse.TeamSummary(t.getId(), t.getName(), p, t.getMembers().size());
                })
                .sorted(Comparator.comparing(DaySummaryResponse.TeamSummary::getTeamName))
                .collect(Collectors.toList());

        long present = per.stream().mapToLong(DaySummaryResponse.TeamSummary::getPresent).sum();
        long total = per.stream().mapToLong(DaySummaryResponse.TeamSummary::getTotal).sum();

        return new DaySummaryResponse(date, per, present, total);
    }

    /* helpers */
    private Team team(String id) {
        return teamRepo.findById(id).orElseThrow(() -> new NoSuchElementException("team not found: " + id));
    }

    private TeamResponse toTeamResponse(Team t) {
        var ms = t.getMembers().stream().map(m -> new MemberResponse(m.getId(), m.getName())).toList();
        return new TeamResponse(t.getId(), t.getName(), t.getLead(), ms);
    }
}