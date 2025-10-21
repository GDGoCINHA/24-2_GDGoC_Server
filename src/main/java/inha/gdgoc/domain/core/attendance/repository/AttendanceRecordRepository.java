package inha.gdgoc.domain.core.attendance.repository;

import inha.gdgoc.domain.core.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    /* 조회: 특정 meetingId의 (userId, present) 목록 */
    @Query("""
                select ar.user.id, ar.present
                from AttendanceRecord ar
                where ar.meeting.id = :meetingId
            """)
    List<Object[]> findPresencePairsByMeetingId(@Param("meetingId") Long meetingId);

    /* 삭제: 해당 meetingId의 출석 레코드 전체 삭제 */
    void deleteByMeeting_Id(Long meetingId);

    /* 배치 업서트(ON CONFLICT) — meeting_id 기준 */
    @Modifying
    @Query(value = """
                INSERT INTO public.attendance_records (meeting_id, user_id, present, updated_at)
                SELECT :meetingId, u, :present, NOW()
                FROM unnest(:userIds) AS u
                ON CONFLICT (meeting_id, user_id)
                DO UPDATE SET present = EXCLUDED.present, updated_at = NOW()
            """, nativeQuery = true)
    int upsertBatchByMeetingId(@Param("meetingId") Long meetingId, @Param("userIds") List<Long> userIds, @Param("present") boolean present);
}