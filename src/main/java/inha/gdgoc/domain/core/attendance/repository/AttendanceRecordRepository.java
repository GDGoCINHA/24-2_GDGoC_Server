// inha/gdgoc/domain/core/attendance/repository/AttendanceRecordRepository.java
package inha.gdgoc.domain.core.attendance.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
@Repository
public interface AttendanceRecordRepository {
    void setPresence(String dateKey, String teamId, String memberId, boolean present);
    long setAll(String dateKey, String teamId, Iterable<String> memberIds, boolean present);

    Map<String, Map<String, Boolean>> getDay(String dateKey); // day -> teamId -> memberId -> present
    void removeDate(String dateKey);
    void removeMemberEverywhere(String teamId, String memberId);
    void removeTeamEverywhere(String teamId);
}